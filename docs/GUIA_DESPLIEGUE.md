# Guía de despliegue

## Etapas

1. Piloto en un Cloud Server Linux propio, con Donweb como opción a evaluar comercialmente.
2. Migración posterior al subdominio e infraestructura que defina la Municipalidad de Ensenada.

La aplicación no usa APIs de Donweb: Docker, variables y dumps MySQL permiten moverla sin cambiar el dominio de negocio.

## Capacidad inicial

Para pocos usuarios, cotizar como punto de partida 2 vCPU, 4 GB RAM y 30 GB de disco. Es una estimación, no una garantía. Medir CPU, RAM, disco, latencia y crecimiento de backups antes de vender capacidad. Evitar hosting compartido: Spring Boot necesita JVM, procesos y red privada.

## Topología

```text
Internet -> HTTPS/DNS -> proxy público del host -> frontend:8080
                                                -> /api -> backend:8080 privado
                                                          -> mysql:3306 privado
```

Compose publica solo `APP_PORT` del frontend. El proxy público puede ser Caddy, Traefik o Nginx administrado por el host y debe renovar TLS automáticamente.

## Preparación del servidor

1. Crear usuario operativo sin login root directo y usar claves SSH.
2. Instalar actualizaciones, Docker Engine y plugin Compose.
3. Permitir en firewall solamente SSH restringido, HTTP y HTTPS.
4. Clonar una versión etiquetada del repositorio.
5. Copiar `.env.example` a `.env`, generar secretos únicos y fijar `PUBLIC_ORIGIN=https://<dominio-piloto>`.
6. Mantener `DEMO_DATA_ENABLED=false`, `FLYWAY_BASELINE_ON_MIGRATE=false` y bootstrap apagado después de crear la primera cuenta.

## Primera publicación

```bash
docker compose config --quiet
docker compose build
docker compose up -d
docker compose ps
docker compose logs --tail=100 backend frontend
curl --fail http://127.0.0.1:8080/healthz
curl --fail http://127.0.0.1:8080/api/health
```

Configurar el proxy TLS hacia `127.0.0.1:${APP_PORT}`. Verificar redirección HTTP→HTTPS, certificado, CSP, HSTS, rutas Angular y login. No publicar directamente 8080 al mundo si el proxy corre en el mismo host; limitarlo a loopback o firewall.

## Aprovisionamiento inicial

Habilitar temporalmente `BOOTSTRAP_SUPERUSER_ENABLED=true` con DNI y contraseña fuertes, levantar backend una vez, iniciar sesión y cambiar contraseña. Luego volver a `false`, quitar las variables sensibles y recrear el backend. Nunca conservar credenciales de bootstrap como configuración permanente.

## Actualización

1. Confirmar CI verde y versión exacta.
2. Ejecutar `sh ops/backup-mysql.sh` y copiar el backup fuera del host.
3. Probar migraciones en un ambiente previo con copia reciente.
4. Construir imágenes antes de la ventana de cambio.
5. Ejecutar `docker compose up -d` y observar health/logs.
6. Completar smoke tests funcionales.
7. Registrar versión, hora, operador y resultado.

No usar automáticamente `FLYWAY_BASELINE_ON_MIGRATE=true` para una base importada; seguir `GUIA_MIGRACIONES.md`.

## Backups

Programación diaria de ejemplo:

```cron
15 3 * * * cd /opt/red-clubes && BACKUP_RETENTION_DAYS=14 sh ops/backup-mysql.sh >> /var/log/red-clubes-backup.log 2>&1
```

Sincronizar después a almacenamiento externo cifrado. Semanalmente revisar existencia/tamaño; mensualmente restaurar el último dump en un entorno aislado y ejecutar health, login y conteos. El script de restore exige `CONFIRM_RESTORE=RESTAURAR` porque sobrescribe datos lógicamente.

## Rollback

Conservar la imagen/versión anterior. Si una publicación falla antes de una migración incompatible, desplegar esa versión. Volver el binario no revierte una migración; para una transformación destructiva se requiere el dump verificado y una decisión explícita. Las migraciones actuales son aditivas salvo V4, que revoca sesiones efímeras.

## Smoke tests

- Health del proxy y backend.
- Login/logout y recarga de pestaña.
- Administrador A no accede a recursos de Club B.
- Alta/baja de socio y auditoría.
- Actividad con profesor, inscripción y regla de cupo.
- Asistencia de fecha válida y rechazo de no inscripto.
- Generación mensual repetida sin duplicar, pago, historial y anulación.
- Dashboard mensual y reporte anual.
- Navegación directa a rutas Angular y refresh.

## Migración a `ensenada.gov.ar`

Coordinar con el área municipal: nombre exacto, DNS, TLS, infraestructura, firewall/proxy, cuentas, backups, monitoreo, incidentes, retención y protección de datos. Preparar un ambiente previo separado; exportar el piloto, verificar checksums/conteos, importar, ejecutar Flyway y smoke tests, congelar escrituras durante el corte y conservar rollback acordado. Cambiar `PUBLIC_ORIGIN` y secretos, no URLs compiladas de Angular.

## Pendientes externos

- Contratar y endurecer el Cloud Server.
- Elegir dominio piloto y configurar TLS/DNS.
- Ejecutar build real de imágenes y restore MySQL en Docker Linux.
- Definir monitoreo externo, alertas, responsable operativo y destino de backups.
