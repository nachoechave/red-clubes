param(
    [string]$BaseUrl = 'http://127.0.0.1:18080',
    [string]$MailUrl = 'http://127.0.0.1:18025'
)
$ErrorActionPreference = 'Stop'
# A previous disposable run must not supply a stale OTP before the new mail arrives.
$null = Invoke-RestMethod -Method Delete "$MailUrl/api/v1/messages"
# Run only against the disposable database created by compose.portal-test.yml.
# Fixed credentials and identities below are fictional, never production data.
function Api($Method, $Path, $Body = $null, $Token = $null) {
    $arguments = @{ Method=$Method; Uri="$BaseUrl$Path"; ContentType='application/json' }
    if ($null -ne $Body) { $arguments.Body = ConvertTo-Json $Body -Depth 5 -Compress }
    if ($Token) { $arguments.Headers = @{ Authorization="Bearer $Token" } }
    # Explicitly enumerate REST arrays; the cmdlet otherwise emits them as one pipeline object.
    foreach ($item in (Invoke-RestMethod @arguments)) { $item }
}
function ExpectStatus($Expected, [scriptblock]$Action) {
    try { & $Action | Out-Null; throw "Se esperaba HTTP $Expected" }
    catch {
        if ($null -eq $_.Exception.Response -or [int]$_.Exception.Response.StatusCode -ne $Expected) { throw }
    }
}
$admin = Api POST '/api/auth/login' @{ dni='99887766'; password='fictional-local-admin-only' }
$club = Api POST '/api/clubes' @{ nombre='Bomberos ensayo Fase 1'; direccion='Calle ficticia 123' } $admin.token
if ($club.id -ne 1) { throw 'La base de ensayo debe estar vacia y PORTAL_CLUB_ID debe ser 1.' }
$otherClub = Api POST '/api/clubes' @{ nombre='Otra institucion ficticia'; direccion='Otra calle ficticia 456' } $admin.token
$ana = Api POST "/api/clubes/$($club.id)/socios" @{
    nombre='Ana'; apellido='Ficticia'; dni='12345678'; estado='ACTIVO'; email='ana@example.test'
} $admin.token
$berta = Api POST "/api/clubes/$($club.id)/socios" @{
    nombre='Berta'; apellido='Ficticia'; dni='23456789'; estado='ACTIVO'; email='berta@example.test'
} $admin.token
$carla = Api POST "/api/clubes/$($otherClub.id)/socios" @{
    nombre='Carla'; apellido='Ficticia'; dni='12345678'; estado='ACTIVO'; email='carla@example.test'
} $admin.token
$today = Get-Date
foreach ($fixture in @(
    @{ Socio=$ana; Club=$club; Periodo='2026-01'; Vencimiento=$today.AddDays(-2).ToString('yyyy-MM-dd') },
    @{ Socio=$ana; Club=$club; Periodo='2026-02'; Vencimiento=$today.AddDays(10).ToString('yyyy-MM-dd') },
    @{ Socio=$ana; Club=$club; Periodo='2026-03'; Vencimiento=$today.AddDays(5).ToString('yyyy-MM-dd') },
    @{ Socio=$berta; Club=$club; Periodo='2026-01'; Vencimiento=$today.AddDays(10).ToString('yyyy-MM-dd') },
    @{ Socio=$carla; Club=$otherClub; Periodo='2026-01'; Vencimiento=$today.AddDays(10).ToString('yyyy-MM-dd') }
)) {
    $createdCuota = Api POST "/api/clubes/$($fixture.Club.id)/cuotas" @{
        socioId=$fixture.Socio.id; periodo=$fixture.Periodo; importe=1500.75
        fechaEmision='2026-01-01'; vencimiento=$fixture.Vencimiento
    } $admin.token
    if ($fixture.Periodo -eq '2026-03') {
        $null = Api POST "/api/clubes/$($fixture.Club.id)/cuotas/$($createdCuota.id)/pagos" @{
            importe=1500.75; medioPago='EFECTIVO'; observaciones='Pago administrativo ficticio previo al portal'
        } $admin.token
    }
    if ($fixture.Periodo -eq '2026-02') {
        $null = Api POST "/api/clubes/$($fixture.Club.id)/cuotas/$($createdCuota.id)/pagos" @{
            importe=500.25; medioPago='EFECTIVO'; observaciones='Pago parcial administrativo ficticio'
        } $admin.token
    }
}
$null = Api POST "/api/portal-bomberos-administracion/socios/$($ana.id)/correo-verificado" @{
    referenciaVerificacion='Acta ficticia de ensayo presencial 001'
} $admin.token
$request = Api POST '/api/portal-bomberos/auth/solicitar-codigo' @{ dni='12345678' }
if ($request.codigo -or $request.email) { throw 'Se filtraron datos en la respuesta publica' }
$message = $null
for ($i=0; $i -lt 20; $i++) {
    $inbox = Invoke-RestMethod "$MailUrl/api/v1/messages"
    if ($inbox.messages.Count -gt 0) {
        $message = Invoke-RestMethod "$MailUrl/api/v1/message/$($inbox.messages[0].ID)"
        break
    }
    Start-Sleep -Milliseconds 500
}
if (!$message -or $message.To[0].Address -ne 'ana@example.test') { throw 'No se recibio el correo de Ana' }
$code = [regex]::Match($message.Text, '\b[0-9]{6}\b').Value
if (!$code) { throw 'El correo no contiene el codigo esperado' }
$session = Api POST '/api/portal-bomberos/auth/verificar-codigo' @{ solicitudId=$request.solicitudId; codigo=$code }
$me = Api GET '/api/portal-bomberos/me' $null $session.token
if ($me.nombre -ne 'Ana' -or $me.PSObject.Properties.Name -contains 'dni') { throw 'Perfil incorrecto' }
$cuotas = @(Api GET '/api/portal-bomberos/cuotas' $null $session.token)
if ($cuotas.Count -ne 3 -or @($cuotas | Where-Object estado -eq 'VENCIDA').Count -ne 1 -or
    @($cuotas | Where-Object estado -eq 'PAGADA').Count -ne 1 -or
    @($cuotas | Where-Object estado -eq 'PENDIENTE').Count -ne 1) { throw 'Cuotas incorrectas' }
if (@($cuotas | Where-Object importe -ne 1500.75).Count -ne 0) { throw 'Importe incorrecto' }
$partial = $cuotas | Where-Object periodo -eq '2026-02'
if ($partial.pagado -ne 500.25 -or $partial.saldoPendiente -ne 1000.50) { throw 'Saldo parcial incorrecto' }
$paid = $cuotas | Where-Object periodo -eq '2026-03'
if ($paid.pagado -ne 1500.75 -or $paid.saldoPendiente -ne 0) { throw 'Saldo de cuota pagada incorrecto' }
ExpectStatus 400 { Api GET "/api/portal-bomberos/cuotas?socioId=$($berta.id)" $null $session.token }
ExpectStatus 401 { Api GET '/api/clubes' $null $session.token }
ExpectStatus 401 { Api POST '/api/portal-bomberos/auth/verificar-codigo' @{ solicitudId=$request.solicitudId; codigo=$code } }
$null = Api POST '/api/portal-bomberos/auth/cerrar-sesion' $null $session.token
ExpectStatus 401 { Api GET '/api/portal-bomberos/me' $null $session.token }
Write-Output 'OK: MySQL, Flyway, activacion auditada, SMTP, OTP, cuotas propias, saldos parciales, aislamiento y logout.'
