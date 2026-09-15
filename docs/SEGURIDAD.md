# Seguridad

Fecha de corte: 15 de septiembre de 2026. Alcance: controles implementados en el repositorio; no certifica la configuración del host futuro.

## Autenticación y sesiones

Spring Security protege `/api/**` por defecto. Solo `POST /api/auth/login`, `GET /api/health` y `OPTIONS` son públicos. La API no usa la sesión HTTP: recibe un bearer opaco de 256 bits, persiste únicamente su SHA-256, permite logout/revocación y elimina sesiones vencidas periódicamente.

Las contraseñas usan `PBKDF2WithHmacSHA256`, salt aleatorio de 128 bits, salida de 256 bits y 600.000 iteraciones. El formato persistido incluye iteraciones, salt y hash. Por eso los valores históricos de 120.000 iteraciones siguen validándose; tras un login correcto se genera y guarda un hash nuevo sin forzar un cambio masivo. Los formatos inválidos se rechazan sin producir un error interno ni aceptar costos arbitrarios.

## Defensa del login

Hay dos capas complementarias:

1. Nginx limita por IP real la ruta exacta `/api/auth/login` y devuelve `429` al exceder el ritmo.
2. El backend limita por una clave SHA-256 de DNI normalizado + IP. Los valores por defecto son cinco fallos en quince minutos y bloqueo de quince minutos. Usuario inexistente, contraseña errónea y bloqueo responden igual (`401`) para evitar enumeración.

El contador del backend vive en memoria. Es correcto para el despliegue de una sola instancia definido en Compose; con dos o más réplicas debe moverse a Redis/base compartida o al proxy perimetral. Un reinicio también limpia el contador, por lo que el límite de Nginx sigue siendo necesario.

## IP real y reverse proxy

Compose publica el frontend exclusivamente en loopback. Nginx confía `X-Forwarded-For` solo si la conexión llega desde `TRUSTED_PROXY_CIDR`, cuyo valor por defecto es el gateway exacto de la red Docker (`172.30.0.1/32`). `real_ip_recursive` selecciona el último salto no confiable, el rate limit usa esa IP y Nginx normaliza los headers antes de enviarlos a Spring.

Condiciones del modelo:

- El reverse proxy público debe ejecutarse en el mismo host y conectarse a `127.0.0.1:APP_PORT`.
- Debe **sobrescribir o anexar correctamente** `X-Forwarded-For` y fijar `X-Forwarded-Proto`.
- `DOCKER_SUBNET`, `DOCKER_GATEWAY` y `TRUSTED_PROXY_CIDR` deben permanecer coherentes.
- No se debe ampliar la confianza a rangos privados completos ni publicar `APP_PORT` a Internet.
- Si el proxy se mueve a otro contenedor/red, se debe confiar su IP/CIDR exacto y volver a probar spoofing y rate limit.

Spring usa `server.forward-headers-strategy=framework`; no debe exponerse directamente porque entonces un cliente podría influir en headers reenviados.

## Autorización y aislamiento multi-club

El filtro autentica al usuario activo y los servicios verifican rol, asignación al club y pertenencia de cada recurso. Sin bearer válido se responde `401`; con sesión válida sin permiso, `403`. Socios, actividades, inscripciones, asistencias, cuotas, pagos y usuarios tienen pruebas de aislamiento. El backend es la autoridad: guards Angular solo mejoran la experiencia.

## Navegador, API y errores

Nginx agrega CSP, `nosniff`, denegación de framing, políticas de referrer/permisos/origen y HSTS. La CSP permite estilos inline porque la aplicación Angular actual los requiere; retirar esa excepción exige una tarea específica de frontend. El HTML principal usa `no-store` y los assets versionados pueden cachearse.

El tamaño de request es 2 MB en Nginx y Spring. Producción no incluye stack traces, mensajes internos ni whitelabel. Los logs estructurados no deben incluir bearer, contraseñas, hashes o cuerpos completos.

CSRF permanece desactivado porque el bearer se envía explícitamente en `Authorization` y no en una cookie automática. Si se migra a cookies deben configurarse `HttpOnly`, `Secure`, `SameSite` y CSRF antes de desplegar.

## Contenedores y red

- Backend y frontend corren como usuarios no root, sin privilegios nuevos y con filesystem de solo lectura salvo `/tmp` acotado.
- MySQL y backend no publican puertos. La red Compose es `internal`.
- Hay límites de CPU, memoria y procesos compatibles con un host inicial de 2 vCPU/4 GB.
- Java limita heap por porcentaje de memoria y termina ante OOM.
- El driver `json-file` rota por tamaño y cantidad; no se permiten logs ilimitados.
- MySQL usa `utf8mb4`, zona horaria coherente y el usuario de aplicación, no root.

## Secretos y cadena de suministro

`.env`, backups, artefactos y claves locales están ignorados. `.env.example` solo contiene placeholders. La inspección de nombres versionados no encontró `.env`, claves privadas ni almacenes de claves. Como el historial puede contener material retirado, cualquier credencial que alguna vez se haya compartido o versionado debe rotarse; borrar un archivo no invalida el secreto.

CI ejecuta:

- Gitleaks sobre el historial Git completo.
- `npm audit --audit-level=high`.
- OWASP Dependency-Check para Maven con umbral CVSS 7.
- Trivy sobre imágenes backend/frontend, severidades `HIGH,CRITICAL`, incluidos hallazgos sin fix.
- Dependabot semanal para Actions, Maven, npm y Docker.

Un hallazgo bloqueante se corrige o se suprime solo con justificación, vencimiento y evidencia de falso positivo/riesgo aceptado. Nunca se copia un secreto real a una issue, log o PR.

## Backups y datos personales

Los dumps son archivos sensibles. Se crean con permisos restrictivos, de forma atómica, se validan y se acompañan por SHA-256. La retención local por defecto es catorce días, pero una copia en el mismo host no es recuperación ante desastre. Usar el hook externo con cifrado `age` (destinatario público) y almacenamiento fuera del servidor. La clave privada de descifrado no debe residir junto al backup.

Cada mes se debe ejecutar `ops/restore-test.sh` sobre el último backup. El script usa un contenedor MySQL descartable, sin puerto y una base `restore_test`; nunca apunta a producción.

## Respuesta mínima a incidentes

1. Aislar el servicio o bloquear el origen sin destruir evidencia.
2. Registrar hora, versión, host, indicador y operador.
3. Rotar secretos afectados y revocar sesiones.
4. Preservar logs y backup cifrado según la política legal/municipal.
5. Corregir en rama, pasar CI y desplegar una versión inmutable.
6. Restaurar solo desde un backup cuyo checksum y restore aislado hayan pasado.
7. Documentar causa, alcance, datos afectados y acciones preventivas.

## Checklist antes del piloto

- [ ] Todos los secretos son únicos y fueron generados fuera de Git.
- [ ] SSH sin root/contraseña, firewall solo 22 restringido, 80 y 443.
- [ ] `APP_PORT` solo aparece en `127.0.0.1`.
- [ ] DNS, TLS y renovación automática verificados.
- [ ] Proxy sobrescribe headers y la IP real observada es correcta.
- [ ] Gitleaks, dependencia e imágenes pasan en CI.
- [ ] Dos clubes de prueba confirman aislamiento 401/403/404.
- [ ] Backup externo cifrado y restore mensual probado.
- [ ] Alertas, responsables, retención e incidentes tienen dueño.
