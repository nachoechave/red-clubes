# Portal Bomberos — Fase 1

Alcance: identificación de socios y lectura de cuotas existentes. Sin Provincia NET,
webhooks, pagos nuevos, contraseñas de socios ni cambios en la web institucional.

## Arquitectura

`portal` es un módulo del backend existente. Reutiliza `Socio`, `Club`, `Cuota`,
sus importes `BigDecimal`, el cálculo de saldo de `CobranzaService`, `SessionTokenService`, los permisos administrativos y
`AuditoriaService`. La consulta de cuotas restringe en SQL tanto institución como
socio, sin cargar las cuotas de otras personas. Calcula PENDIENTE → VENCIDA con la
fecha de Buenos Aires sin cambiar cuotas pagadas o anuladas ni escribir importes.
Los pagos administrativos siguen siendo la fuente de los estados PAGADA/ANULADA.
El portal también entrega `pagado` y `saldoPendiente`: los pagos parciales y las
anulaciones usan el mismo cálculo que la administración, sin duplicar reglas.

Una cadena Spring Security exclusiva del portal acepta solo sus rutas y sesiones.
Un token del portal nunca autentica un usuario administrativo y viceversa.
`GET /me` devuelve nombre y número de socio, sin apellido, DNI, correo ni domicilio.

V10 agrega tres tablas nuevas; ninguna migración anterior se modifica:

- `portal_verified_email`: evidencia institucional de verificación, correo exacto,
  institución, operador, fecha, referencia y revisión aleatoria.
- `portal_challenge`: solicitud aleatoria, HMAC del OTP, vencimiento, contador y consumo.
  Las solicitudes no habilitadas también generan un desafío sin socio.
- `portal_session`: SHA-256 del token opaco de 256 bits, socio, institución,
  revisión de verificación, vencimiento y revocación.

El OTP tiene seis dígitos aleatorios uniformes y dura exactamente cinco minutos.
El HMAC-SHA256 usa `PORTAL_SECRET` y el identificador de solicitud; una copia de la
base sin ese secreto no permite enumerar offline el millón de códigos posibles.
La comparación es constante para hashes de igual longitud. El token se entrega
solo al verificar correctamente; nunca se devuelve ni registra el OTP en la API.

La solicitud pública siempre devuelve HTTP 202, el mismo mensaje y un identificador
aleatorio, incluso cuando no existe el DNI, no hay correo verificado, el portal está
deshabilitado o el límite rechaza la solicitud. No devuelve correos enmascarados.
El SMTP se ejecuta después del commit en una cola acotada (100 pendientes, hasta
dos trabajadores); el tiempo o fallo del proveedor no cambia la respuesta pública.
No se promete indistinguibilidad criptográfica de tiempos de consultas SQL.

Los bloqueos pesimistas serializan por socio y desafío; las verificaciones usan
READ COMMITTED y no cargan un desafío antes de bloquearlo. Los errores de
autenticación conservan el contador de intentos en la transacción. Un OTP usado
no se puede reutilizar, y un reenvío admitido consume el desafío anterior.

## Activación asistida y revocación

Los correos existentes empiezan **sin verificar**. Antes de activar acceso, la
institución debe comprobar identidad y control del buzón mediante su procedimiento
asistido (por ejemplo atención presencial y confirmación del correo con el socio).
Registrar una referencia de acta/ticket, sin copiar documentación personal en ella.
Este endpoint registra la declaración de un administrador: no sustituye ese control.

Solo un superusuario o administrador asignado a Bomberos puede ejecutar:

```http
POST /api/portal-bomberos-administracion/socios/123/correo-verificado
Authorization: Bearer <token-administrativo>
Content-Type: application/json

{"referenciaVerificacion":"Acta de verificación 001","revocar":false}
```

Responde 204, genera auditoría y vincula el acceso al correo registrado exacto.
Con `revocar:true` invalida acceso y códigos. Se puede revocar aunque el correo
haya sido eliminado. Cada activación/revocación cambia la revisión: reactivar nunca
resucita sesiones antiguas. Cambiar el correo, dar de baja al socio/institución o
moverlo a otra institución impide su autenticación. Una petición ya autorizada que
está en curso puede terminar antes de observar una revocación concurrente.

## Contrato HTTP

Todas las respuestas del portal llevan `Cache-Control: no-store`.
Datos ficticios de ejemplo; nunca enviar identificadores de otros socios.

```http
POST /api/portal-bomberos/auth/solicitar-codigo
Content-Type: application/json

{"dni":"12345678"}
```

```json
{"solicitudId":"b6e42897-dcde-4599-8baf-f48b43a1a4b2","mensaje":"Si el acceso esta habilitado, recibiras un codigo. Si necesitas ayuda, contacta a la institucion."}
```

```http
POST /api/portal-bomberos/auth/verificar-codigo
Content-Type: application/json

{"solicitudId":"b6e42897-dcde-4599-8baf-f48b43a1a4b2","codigo":"123456"}
```

Respuesta 200 (el token real es aleatorio); todos los fallos de autenticación,
vencimientos, bloqueos y reutilización devuelven el mismo 401:

```json
{"token":"<token-opaco>","tokenType":"Bearer","expiresAt":"2026-10-26T13:15:00Z"}
```

```http
GET /api/portal-bomberos/me
Authorization: Bearer <token-opaco>
```

```json
{"nombre":"Ana","numeroSocio":1}
```

```http
GET /api/portal-bomberos/cuotas
Authorization: Bearer <token-opaco>
```

```json
[{"id":1,"periodo":"2026-10","importe":1500.75,"pagado":500.25,"saldoPendiente":1000.50,"vencimiento":"2026-10-10","estado":"VENCIDA"}]
```

La lista incluye cuotas pagadas, pendientes, vencidas y anuladas. Es de solo lectura.
`socioId`, `clubId` y cualquier otro query parameter se rechazan con 400.

```http
POST /api/portal-bomberos/auth/cerrar-sesion
Authorization: Bearer <token-opaco>
```

Responde 204 y revoca la sesión; el token deja de servir. Las sesiones vencen a los
15 minutos por defecto, sin renovación deslizante. Una limpieza cada 10 minutos
elimina desafíos/sesiones vencidos; su autenticación se rechaza inmediatamente al
vencer aunque la limpieza todavía no haya corrido. Cuerpos inválidos reciben 400.

## Configuración

El portal está apagado por defecto. Configurar `.env.example` sin subir `.env`:

| Variable | Valor / significado |
| --- | --- |
| PORTAL_ENABLED | true para habilitar |
| PORTAL_CLUB_ID | ID real de la institución Bomberos; nunca se busca por nombre |
| PORTAL_SECRET | Secreto aleatorio independiente de al menos 32 caracteres; no usar el de ensayo |
| PORTAL_ALLOWED_ORIGINS | Orígenes exactos de la web, separados por coma; default http://localhost:4300 |
| PORTAL_SESSION_MINUTES | 15; aceptados 1–60 |
| PORTAL_MAX_ATTEMPTS | 5 por desafío; aceptados 1–10 |
| PORTAL_IDENTITY_REQUESTS | 3 solicitudes admitidas por DNI cada 15 minutos |
| PORTAL_IP_REQUESTS | 20 solicitudes admitidas por IP cada 15 minutos |
| PORTAL_COOLDOWN_SECONDS | 60 entre reenvíos admitidos de la misma identidad |
| PORTAL_MAIL_FROM | Remitente autorizado por el proveedor |
| SMTP_HOST / SMTP_PORT | Servidor SMTP; prueba localhost:1025; producción según proveedor |
| SMTP_USERNAME / SMTP_PASSWORD | Credenciales del proveedor, solo entorno/gestor de secretos |
| SMTP_AUTH | true cuando el proveedor requiere autenticación |
| SMTP_STARTTLS / SMTP_STARTTLS_REQUIRED | Configurar ambas true con SMTP STARTTLS productivo |

OTP: 5 minutos fijos. Verificación: 60 solicitudes por IP y 20 por identidad cada
15 minutos, además de los cinco intentos persistidos por desafío. Contadores
separados por IP/identidad evitan eludirlos cambiando una de las dos. Las claves
son HMAC, sin DNI/IP en claro; máximo 10.000 entradas, rechazando nuevas claves al
agotarse la capacidad. Los excesos de solicitud conservan 202; los de verificación,
401. Timeouts SMTP: conexión, lectura y escritura de cinco segundos.

CORS del portal es independiente del administrativo, sin wildcard ni cookies.
La web debe enviar Authorization explícitamente y conservar el bearer en memoria
de la pestaña; evitar almacenamiento persistente. CSRF sigue deshabilitado por ser
bearer explícito. No migrar a cookies sin añadir protección CSRF. Usar HTTPS fuera
del ensayo local. Si la web usa CSP, agregar el origen de la API a su `connect-src`.
La configuración del sitio Web Bomberil corresponde a su repositorio separado.

Compose pasa las variables y permite salida SMTP al backend en `edge`, manteniendo
MySQL solo en `private` y sin publicar backend/MySQL. Verificar firewall y TLS del
proveedor. No enviar correo real a través de un relay de prueba sin autenticación.

## Límites operativos

Los desafíos, intentos y sesiones se coordinan en MySQL entre instancias, pero el
rate limiter y la cola SMTP son locales. Este diseño admite **una sola instancia**
del backend. Un reinicio borra los límites en memoria y correos pendientes; el OTP
no se registra ni recupera en texto plano. Se puede pedir otro código tras el límite.
Antes de escalar o hacer reinicios frecuentes, mover los contadores a Redis/base
compartida y la entrega a una cola duradera cifrada, con límites en el proxy público.
Agregar defensa perimetral contra tráfico distribuido. La aplicación limita IP y
DNI; no protege por sí sola contra una botnet con muchas identidades.

La IP proviene de `getRemoteAddr` normalizada por la configuración de proxy existente.
El proxy público debe sobrescribir Forwarded/X-Forwarded-For y confiar solo saltos
medidos. No exponer Spring directamente a Internet ni confiar headers arbitrarios.
Ver `SEGURIDAD.md` y `PROXY_Y_RATE_LIMIT.md`. Monitorizar los avisos SMTP sin cuerpos,
destinatarios, códigos, tokens ni excepciones del proveedor.

## Ensayo reproducible con datos ficticios

Requiere Docker Linux activo. Usar puertos locales 13316, 18025 y 18080 libres.
El Compose de ensayo usa exclusivamente credenciales y datos ficticios.

```powershell
docker compose -p portal-phase1 -f ops/compose.portal-test.yml up --build -d
# Esperar GET http://127.0.0.1:18080/api/health -> 200.
powershell -NoProfile -File ops/test-portal-mysql.ps1
```

Abrir el buzón en http://127.0.0.1:18025. El script crea dos instituciones, tres
socios y cinco cuotas vía API administrativa, incluyendo un pago completo y uno parcial
ficticio; activa solo a Ana, recibe el correo SMTP, verifica el código, lee sus tres
cuotas (pagada, pendiente y vencida) y comprueba aislamiento/reutilización/
logout. No imprime códigos ni tokens. Ejecutar una vez por base vacía.

Para repetir, borrar **solo el proyecto descartable de ensayo**:

```powershell
docker compose -p portal-phase1 -f ops/compose.portal-test.yml down -v
docker compose -p portal-phase1 -f ops/compose.portal-test.yml up --build -d
```

Ejecución manual: Java 21 y MySQL nuevo, exportar las variables de `.env.example`,
`DB_URL/DB_USERNAME/DB_PASSWORD` y ejecutar `backend/mvnw.cmd spring-boot:run` desde
`backend`. Spring no carga automáticamente el archivo `.env`. SMTP de prueba puede
ser Mailpit; usar `SMTP_HOST=localhost`, `SMTP_PORT=1025` y auth/STARTTLS false.

Pruebas automatizadas:

```powershell
cd backend
.\mvnw.cmd clean verify
```

La suite usa Flyway + H2 en modo MySQL y prueba también concurrencia real con
transacciones separadas. El script adicional verifica el motor MySQL y SMTP reales.

## Base e integración

La rama original `feature/bomberos-portal-api` partió del commit local `52179f6`.
El seguimiento integró después los cambios pendientes de solicitudes, grupos,
importación y pagos parciales, junto con `origin/main`, en el commit de base
`bc831aa`. Los archivos pendientes se respaldaron antes de confirmar esa base.

El historial integrado usa la V7 oficial de tipo de institución, V8 de grupos,
V9 de solicitudes y V10 del portal. V7 coincide exactamente con `origin/main`;
V8 contiene exactamente el SQL de la antigua V7 local de grupos. La instalación
local ya tenía el historial reconciliado hasta V9; se comprobó solo por lectura,
sin cambiar datos ni su historial. Para otras instalaciones con la V7 antigua,
seguir el procedimiento de `INTEGRACION_WEB_BOMBEROS.md` sobre una copia restaurada.
Nunca reemplazar una migración aplicada ni ejecutar Flyway repair a ciegas.

Los conflictos de CORS y del filtro se resolvieron conservando las tres superficies:
solicitudes públicas, portal del socio y administración. Aprobar una solicitud
crea el socio pero no activa su correo; la activación asistida sigue siendo necesaria.

## Evidencia de validación — 26 de octubre de 2026

- Java 21.0.13, Maven 3.9.15: `mvn verify`, **81 pruebas, cero fallos, cero errores,
  cero omitidas**. Incluye 19 pruebas nuevas de integración del portal y las 62
  existentes de administración, roles, cuotas, pagos, grupos y esquema.
- Flyway V1–V7 + V10 y Hibernate `validate` pasaron en H2 modo MySQL.
- MySQL 8.4.13 real, base descartable independiente: Flyway V10 y Hibernate
  `validate` correctos. Mailpit 1.30.0 recibió el correo mediante SMTP real desde
  `JavaMailSender`. `ops/test-portal-mysql.ps1` terminó con `OK`.
- Se consultaron las tres cuotas de Ana (PAGADA, PENDIENTE, VENCIDA), conservando
  1500,75 exactos. Las otras dos cuotas (otro socio y otra institución con el mismo
  DNI) no se expusieron. Se rechazaron `socioId` ajeno, token de portal en API
  administrativa y código reutilizado; logout dejó el token sin acceso.
- `docker compose -p portal-phase1 -f ops/compose.portal-test.yml config --quiet`
  y `git diff --check` correctos.

Las 19 pruebas del portal cubren DNI inexistente, correo no verificado, código
correcto/incorrecto/vencido/usado/bloqueado, contadores concurrentes sin pérdida,
consumo único concurrente, reenvío y límites por IP e identidad, sesión expirada,
logout, revocación institucional y reactivación, cambio de correo y baja,
aislamiento entre instituciones, consultas ajenas, estados e importes,
activación auditada, separación administrativa, limpieza y CORS.

Archivos de la fase:

- Nuevos: `backend/src/main/java/com/redclubes/backend/portal/*.java` (15 archivos),
  `backend/src/main/resources/db/migration/V10__portal_bomberos.sql`,
  `backend/src/test/java/com/redclubes/backend/portal/PortalIntegrationTests.java`,
  esta guía, `ops/compose.portal-test.yml` y `ops/test-portal-mysql.ps1`.
- Actualizados: `.env.example`, `.env.production.example`, `.gitignore`,
  `README.md`, `backend/pom.xml`, `backend/src/main/resources/application.properties`,
  `docker-compose.yml`, `config/CorsConfig.java`, `config/SesionAuthenticationFilter.java`
  y `gestion/CuotaRepository.java` dentro del paquete backend existente.

En el ensayo inicial no se construyó la imagen: el flujo usó el JAR de `verify`,
MySQL y Mailpit aislados. El seguimiento de integración también construyó y probó
el Compose completo, como se detalla debajo. No se validó la entrega de un proveedor
SMTP productivo, el proxy público ni el sitio Web Bomberil externo. No se desplegó
producción.

### Validación posterior de la integración

- Backend combinado: **101 pruebas, cero fallos, cero errores, cero omitidas**.
  Incluye 79 de la base y 22 del portal. Las tres nuevas comprueban pagos parciales
  y anulaciones, inscripción/aprobación sin verificación implícita de correo y
  separación de CORS entre solicitudes, portal y administración.
- Frontend: **29 pruebas aprobadas**, compilación Angular correcta con Node 22.23.2
  y auditoría npm de producción sin vulnerabilidades.
- Se conserva la advertencia previa de tamaño inicial: 618,85 kB frente al umbral
  de advertencia de 500 kB; no supera el límite de error ni impide la compilación.
- Se construyó la imagen del backend combinado y se ejecutó el Compose completo
  bajo el proyecto descartable `portal-integration`. MySQL aplicó V1 a V10 sin
  errores, incluyendo la V7 oficial, V8 de grupos y V9 de solicitudes.
- El ensayo SMTP/OTP/consulta terminó con `OK`: un pago parcial de 500,25 sobre
  1500,75 dejó saldo de 1000,50; la cuota pagada mostró saldo cero. Se mantuvieron
  el aislamiento por socio/institución, el rechazo de reutilización y la revocación
  por logout. La base de la instalación local no recibió migraciones ni escrituras.
