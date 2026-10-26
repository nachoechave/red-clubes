param(
    [ValidatePattern('^[a-zA-Z0-9_.-]+$')][string]$Container = 'red-clubes-mysql-1',
    [string]$BackupPath,
    [switch]$Apply
)
$ErrorActionPreference = 'Stop'
function Query([string]$Sql) {
    $result = $Sql | docker exec -i $Container sh -ec 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql --user="$MYSQL_USER" --database="$MYSQL_DATABASE" --batch --skip-column-names'
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar la base. No continuar.' }
    return ($result -join "`n").Trim()
}
# Solo reconoce la rama local exacta. Nunca usa Flyway repair ni cambia checksums.
$expected = Query "SELECT COUNT(*) FROM flyway_schema_history WHERE version='7' AND script='V7__grupos_y_modulos_por_club.sql' AND checksum=-1963158814 AND success=1;"
$versions = Query "SELECT COUNT(*) FROM flyway_schema_history WHERE version IN ('7','8');"
$typeColumn = Query "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='club' AND column_name='tipo_institucion';"
$modules = Query "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='club_modulo';"
if ($expected -ne '1' -or $versions -ne '1' -or $typeColumn -ne '0' -or $modules -ne '1') {
    throw 'La base no coincide con el historial local conocido. No se modificó el historial; requiere revisión manual.'
}
Write-Output 'Detectada V7 local de grupos. La V7 oficial aún no está aplicada.'
if (!$Apply) { Write-Output 'Sin cambios. Para aplicar: detener backend y usar -Apply -BackupPath <backup.sql.gz>.'; exit 0 }
if (!$BackupPath -or !(Test-Path -LiteralPath $BackupPath -PathType Leaf)) { throw 'Se requiere un backup .sql.gz existente.' }
$stream = [IO.File]::OpenRead((Resolve-Path -LiteralPath $BackupPath).Path)
try {
    $gzip = [IO.Compression.GZipStream]::new($stream, [IO.Compression.CompressionMode]::Decompress)
    try { if ($gzip.ReadByte() -lt 0) { throw 'El backup está vacío.' }; $buffer = [byte[]]::new(8192); while ($gzip.Read($buffer,0,$buffer.Length) -gt 0) {} }
    finally { $gzip.Dispose() }
} finally { $stream.Dispose() }
if ($Container -eq 'red-clubes-mysql-1') {
    $running = docker inspect --format '{{.State.Running}}' red-clubes-backend-1
    if ($LASTEXITCODE -ne 0 -or $running -ne 'false') { throw 'Detener backend antes de reconciliar el historial.' }
}
$rows = Query "UPDATE flyway_schema_history SET version='8', script='V8__grupos_y_modulos_por_club.sql' WHERE version='7' AND script='V7__grupos_y_modulos_por_club.sql' AND checksum=-1963158814 AND success=1; SELECT ROW_COUNT();"
if ($rows -ne '1') { throw 'Resultado inesperado. Mantener backend detenido y revisar el historial.' }
$hash = (Get-FileHash -LiteralPath $BackupPath -Algorithm SHA256).Hash
Write-Output "Historial local reconciliado: grupos V8, checksum conservado. Backup SHA256: $hash"
Write-Output 'Iniciar la nueva versión una vez con FLYWAY_OUT_OF_ORDER=true para aplicar V7 oficial y V9. Después volver a false.'
