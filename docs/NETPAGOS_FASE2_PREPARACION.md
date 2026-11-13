# NET Pagos — preparación de la Fase 2

Estado: implementación de ensayo, deshabilitada por defecto. No autoriza operar
en producción ni certifica homologación de NET Pagos. La web bomberil todavía debe
conectar estas rutas. La Fase 1 y la administración conservan sus rutas.

## Qué está preparado

- Intenciones persistidas, clave idempotente por institución/socio, ítems y reservas
  de cuotas; importes calculados por Red Clubes desde el saldo real.
- Cliente HTTP del backend para POST/GET de preorders, `x-api-key`, importes string
  con dos decimales y `locked_payer=true`. Entre uno y diez ítems, sin cuotas ajenas.
- Referencia externa única por pago, conciliación transaccional y registro usando
  el mismo servicio de cobranza y auditoría que la administración.
- Recepción de webhook como aviso **no autenticado**, deduplicado y sin guardar
  datos del pagador. Nunca acredita dinero ni acepta como verdadero su estado.
- Consulta autenticada al proveedor, actualización a pedido y reconciliador opcional
  en segundo plano. Bandeja administrativa paginada de incidencias.
- Simulador acotado de órdenes ficticias; cliente HTTP probado contra servidor local.
- Migración nueva V11; no se editan V1–V10.

## Modos y barreras

| Configuración | Comportamiento |
| --- | --- |
| `NETPAGOS_MODE=OFF` | Default. No crea órdenes ni consulta al proveedor; webhook cerrado. |
| `SIMULATED` | Proveedor en memoria, sin llamadas externas; URL de checkout ficticia bajo example.invalid. |
| `STAGING` | Solo host oficial de staging y checkout de staging; requiere API key. |

Los modos activos exigen perfil `netpagos-sandbox`, institución configurada en
`PORTAL_CLUB_ID` y ausencia de perfil `prod`. **No existe modo de producción en
esta preparación.** El Compose habitual fuerza prod: intentar habilitar allí NET
Pagos hace fallar la configuración. Usar exclusivamente una base de ensayo vacía.

`NETPAGOS_SETTLEMENT_ENABLED=false` conserva resultados confirmados sin registrar
pagos. Para acreditar dentro del ensayo se requiere `true` y `NETPAGOS_ACTOR_ID`
de un usuario activo que pueda operar la institución; sus acciones quedan auditadas
como NET Pagos. En STAGING también exige `NETPAGOS_CONTRACT_CONFIRMED=true`, que
solo corresponde activar después de confirmar el contrato con el proveedor.
Ese flag **no autentica el webhook** y no habilita producción.

Otras variables:

- `NETPAGOS_API_KEY`: solo entorno/gestor de secretos, jamás Angular o Git.
- `NETPAGOS_WEBHOOK_ENABLED=false`: recepción apagada hasta configurar el ensayo.
- `NETPAGOS_RECONCILIATION_ENABLED=false`: trabajador periódico apagado por defecto.
  Si se habilita, toma hasta cinco órdenes por ronda, cada minuto, y respeta una
  consulta por orden cada treinta segundos. Las filas bloqueadas coordinan réplicas;
  el simulador y el límite de webhook siguen siendo de una sola instancia.

El reconciliador revisa READY, PROCESSING, CONFIRMED y PARTIAL. UNKNOWN sin UUID y
REVIEW requieren intervención administrativa. Los fallos de consulta conservan
la orden y sus reservas, sin inventar cancelaciones. La consulta también puede
ejecutarse desde un socio o administrador sin esperar al trabajador.

## Convenio, pagador y barras

No se genera una barra ficticia para el proveedor real. Un administrador debe
cargar la barra y el identificador `service` obtenidos según el convenio aprobado:

```http
POST /api/netpagos-administracion/cuotas/123/barra
Authorization: Bearer <administrador>
Content-Type: application/json

{"barcode":"<barra numerica del convenio>","service":"<servicio del convenio>","validoHasta":"2026-12-31","referenciaConvenio":"Referencia de validación del convenio"}
```

Responde 204. El backend toma el saldo exacto de la cuota en ese momento: no recibe
importe del navegador. Si cambia el saldo o vence la barra, se exige actualizarla.
La pareja barra/servicio es única por institución para evitar ítems ambiguos. No se
puede modificar la barra mientras hay una reserva. Los límites internos defensivos
son barra numérica de hasta 128 caracteres y servicio de hasta 80; no afirman las
longitudes oficiales del convenio. `validoHasta` debe provenir de ese convenio:
no inferimos ni decodificamos vencimientos dentro de la barra.

Nombre, apellido, correo y DNI salen del socio autenticado, sin devolverlos en la
respuesta de la intención. `document_type` es "1". El campo `generoDocumento`
acepta "1", "2" o "3", declarado explícitamente por el socio; no se infiere del
DNI ni del nombre y no se agrega al padrón. Confirmar con NET cómo obtenerlo o
si puede omitirse antes de conectar una pantalla real.

## Contrato para la web bomberil

```http
POST /api/portal-bomberos/pagos
Authorization: Bearer <sesion-del-socio>
Content-Type: application/json

{"cuotaIds":[123,124],"claveSolicitud":"e4346669-3b88-46ad-b76c-2d4d0b2f57d4","generoDocumento":"3"}
```

El navegador conserva `claveSolicitud` durante reintentos de la misma selección.
Repetirla devuelve la misma intención sin otro POST al proveedor. Reutilizarla con
otra selección o género devuelve 409. Se rechazan campos adicionales como importe,
DNI, socio, barra, servicio o URL. No almacenar datos del pagador en el navegador.

Respuesta 202, ejemplo simulado:

```json
{"id":"<uuid-local>","estado":"READY","total":2501.25,"urlPago":"https://checkout.example.invalid/<uuid>","simulado":true,"incidencia":null,"cuotas":[{"cuotaId":123,"importe":1500.75,"acreditado":false},{"cuotaId":124,"importe":1000.50,"acreditado":false}]}
```

```http
GET /api/portal-bomberos/pagos/<uuid-local>
POST /api/portal-bomberos/pagos/<uuid-local>/actualizar
Authorization: Bearer <sesion-del-socio>
```

GET lee la copia local; POST consulta al proveedor con límite por orden. Ambas
devuelven el mismo contrato y solo admiten al socio y su institución. Un token de
portal no sirve en rutas administrativas. Las respuestas del portal son no-store.
La vuelta del checkout o una URL del navegador nunca registra un pago.

Estados locales:

| Estado | Significado |
| --- | --- |
| CREATING | Preparada y reservada; todavía no se confirmó el resultado de creación. |
| READY / PROCESSING | Intención creada o pago en proceso. |
| UNKNOWN | Resultado del POST desconocido; no reenviar ni liberar automáticamente. |
| CONFIRMED / PARTIAL | Resultado verificado, todavía sin aplicación contable. |
| CANCELLED | Cancelación consultada al proveedor; reservas liberadas. |
| REVIEW | Hay discrepancias o falta información confiable. |
| APPLIED | Se registraron exactamente los ítems confirmados cobrados. |

Ante UNKNOWN no hay un reintento automático del POST: el proveedor no documenta
una clave de idempotencia para creaciones. Un reinicio también puede dejar CREATING;
no se supone que una respuesta perdida signifique una orden fallida. Las reservas
no se liberan por el paso del tiempo, porque el contrato no define la relación entre
vencimiento, acreditación tardía y cancelación. Existe un máximo de diez nuevas
intenciones por socio en quince minutos, persistido en la base.

## Conciliación y el significado de Parcial

`Parcial` indica que ciertos ítems del carrito se cobraron y otros fallaron; no es
un abono fraccionado dentro de una cuota. Los pagos administrativos fraccionados
siguen siendo posibles; NET recibe el saldo pendiente completo de cada ítem.

Para aplicar un resultado se verifica UUID, documento del pagador, total, cantidad
de ítems, barra/servicio e importe exacto de cada uno. Se bloquean todas las cuotas
en orden y se revisa el carrito entero antes de registrar cualquier pago. Cambios
de saldo, pertenencia o cuotas anuladas llevan a REVIEW. Los pagos manuales se
rechazan mientras exista una reserva NET; las anulaciones previas pueden provocar
una discrepancia que requiera revisión. Una referencia externa única y el bloqueo
de la intención impiden dobles acreditaciones. Un pago NET ya registrado y anulado
por administración no se vuelve a registrar al repetir una notificación/consulta.

Para `Finalizado`, la consulta autenticada indica que el carrito se completó; si
el proveedor devuelve algún `paid=false`, se considera contradictorio. Para
`Parcial`, se exige `paid` verificado para cada ítem, con al menos uno true y uno
false. **El ejemplo GET del PDF no incluye ese campo**: sin confirmación adicional
del proveedor, el caso parcial queda en REVIEW y no acredita. La implementación
puede procesarlo si GET lo incorpora de forma confirmada; el simulador lo incluye
para probar esa rama. No se copia la bandera desde un webhook sin autenticar.

APPLIED no retrocede ni genera anulaciones automáticas ante un aviso tardío de
Cancelado; requiere revisión. No se interpreta `total_refund` como un reintegro
bancario confirmado ni se implementan devoluciones sin un contrato específico.

## Webhook preparado, sin inventar autenticación

`POST /api/integraciones/netpagos/webhook`, JSON y máximo 16 KiB. Reconoce
Instanciado/Finalizado/Cancelado/Parcial; acepta únicamente un UUID válido. Responde
202 en este contrato local, también para una orden desconocida sin crear filas
huérfanas. Los avisos conocidos se deduplican por UUID/estado, como pistas de
actualización (máximo cuatro filas por orden). No se guardan payer ni cuerpo crudo.
No pretende ser un historial completo de eventos del proveedor.

La recepción limita sesenta solicitudes por IP cada quince minutos, con memoria
acotada a diez mil claves hash. Configurar también límites en el reverse proxy,
sobrescribir headers reenviados y usar HTTPS público. La firma, credenciales,
certificados, IPs de origen, reintentos y código HTTP esperado no están definidos
en los PDF recibidos: deben confirmarse. No se inventó un header de autenticación
que NET no haya acordado. Hasta entonces un aviso solo provoca una pista durable;
la verdad financiera procede de la consulta al proveedor, nunca de su cuerpo.

## Administración y recuperación

Solo administradores de la institución o superusuarios:

- `GET /api/netpagos-administracion/intenciones?estado=REVIEW&pagina=0`: veinte por
  página. Consultar también UNKNOWN y CREATING para revisar creaciones inciertas.
- `POST /api/netpagos-administracion/intenciones/{id}/conciliar`: consulta limitada
  al proveedor y aplicación solo si las barreras de configuración lo permiten.
- `POST /api/netpagos-administracion/intenciones/{id}/recuperar`, cuerpo
  `{"preorderUuid":"<uuid-confirmado-por-NET>"}`: vincula una orden con resultado
  de creación perdido. Verifica el contenido por GET antes de vincular, sin reenviar
  el POST. Si NET no puede aportar UUID, mantener en revisión y resolver el caso con
  soporte; no hay liberación forzada sin evidencia.
- `POST /api/netpagos-administracion/intenciones/{id}/simular`, cuerpo
  `{"estado":"Parcial","paid":[true,false]}`: solo SIMULATED. Respeta el orden
  ascendente de cuotaId devuelto por la intención. Nunca está disponible en STAGING.

## Ensayo reproducible, sin NET real

Requiere Docker activo y puertos 13316/18025/18080 libres. Usar un proyecto vacío:

```powershell
docker compose -p netpagos-phase2 -f ops/compose.portal-test.yml -f ops/compose.netpagos-test.yml up --build -d
# Esperar http://127.0.0.1:18080/api/health -> 200.
powershell -NoProfile -File ops/test-portal-mysql.ps1 -NetPagos
```

El ensayo crea solo datos ficticios; pasa por correo SMTP/OTP real de prueba y
MySQL, luego por el proveedor simulado sin red externa. Prueba un carrito de
2501,25, reserva contra doble cobro manual, un ítem cobrado y otro fallido,
idempotencia, saldo pendiente del ítem fallido y cierre de sesión. El actor 1 del
overlay es el superusuario ficticio creado en esa base vacía, nunca una cuenta real.

Al terminar, o para repetir desde cero, eliminar **solo el proyecto descartable**:

```powershell
docker compose -p netpagos-phase2 -f ops/compose.portal-test.yml -f ops/compose.netpagos-test.yml down -v
```

El simulador admite hasta mil órdenes y pierde su estado al reiniciar. Esa pérdida
no acredita ni cancela nada en MySQL; para repetir el ensayo se recrea su base.
No conectarlo a la base de una institución real.

Pruebas de backend: Java 21, desde backend, `mvnw.cmd clean verify`. Los tests de
HTTP usan un servidor local, verifican campos, montos string, API key, ausencia de
redirects/reintentos, tamaños acotados y errores sin contenido sensible.

## Confirmaciones pendientes con NET Pagos

1. Versión aplicable: nombre del PDF API v2.2 frente al historial interno 1.2;
   endpoints publicados `/api/v1/service/preorder`, no asumir `/v2`.
2. Alta de Bomberos, API key de staging y autorización de IP pública de salida/NAT.
3. Convenio, `service`, algoritmo/origen y formato de barras, relación con importes
   y vencimientos; confirmar reglas para cuotas ya vencidas o abonadas parcialmente.
4. Requisitos de `gender` y política de datos editables `locked_payer`.
5. Cómo consultar de forma autenticada los resultados individuales de un carrito
   Parcial, ausentes en el ejemplo GET.
6. Autenticación del webhook, endpoint HTTPS, respuesta esperada, reintentos,
   duplicados, orden de eventos, identificador de evento y retención.
7. Idempotencia/recuperación de creación tras timeout, zona horaria de las fechas,
   acreditaciones tardías, cancelaciones y significado operativo de total_refund.

No se accede a NET real con credenciales ficticias, no se habilita producción y
no se da por homologado un contrato que todavía requiere esas respuestas.

## Validación ejecutada — 26 de octubre de 2026

- Java 21 y Maven verify: **141 pruebas aprobadas**, cero fallos, errores u omitidas.
  Incluye las 101 previas y 40 nuevas de NET Pagos: contrato HTTP local,
  configuración, aislamiento, concurrencia, recuperación, estados, reservas,
  aplicación única, carrito parcial y controles de acreditación.
- Frontend con Node 22.23.2: **29 pruebas aprobadas** y compilación correcta. Se
  conserva la advertencia previa de 618,85 kB frente a 500 kB de advertencia.
- Imagen del backend construida y ejecutada con MySQL 8.4 y Mailpit. V1–V11
  aplicadas correctamente, con validación Hibernate. El perfil de la aplicación
  fue netpagos-sandbox, proveedor SIMULATED, base y credenciales ficticias.
- `ops/test-portal-mysql.ps1 -NetPagos` terminó con ambos mensajes OK. El carrito
  de 2501,25 registró exactamente un pago NET de 1500,75; el ítem fallido conservó
  saldo 1000,50. El pago manual concurrente fue rechazado y el reintento no duplicó
  la intención ni el cobro. Correo SMTP/OTP, aislamiento y logout también pasaron.
- La consulta de reservas usa un conteo numérico portable, comprobado en MySQL
  además de H2. La configuración Compose de ensayo y la habitual validaron sin errores.

No se llamó a los servidores de NET Pagos ni se usaron credenciales reales.
No se modificó la base de la instalación local ni se desplegó producción.
