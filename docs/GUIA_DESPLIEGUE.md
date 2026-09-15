# Guía de despliegue

Runbook para un piloto de una sola instancia en Linux, inicialmente en un Cloud Server y portable luego a infraestructura municipal. La aplicación no depende de un proveedor: Docker Compose, variables, imágenes etiquetadas y dumps MySQL son la unidad de traslado.

## Arquitectura objetivo

```text
Internet :443/:80
        -> reverse proxy del host (TLS y redirect)
        -> 127.0.0.1:APP_PORT
        -> Nginx frontend (red Docker interna)
        -> Spring backend:8080 (sin puerto host)
        -> MySQL:3306 + volumen (sin puerto host)
```

Capacidad inicial: 2 vCPU, 4 GB RAM, 30 GB SSD y espacio externo para backups. Compose asigna 0,90 CPU/1536 MB a MySQL, 0,85 CPU/1152 MB al backend y 0,25 CPU/192 MB al frontend, dejando margen al sistema, Docker y proxy. Medir antes de ampliar usuarios.

## Variables de producción

| Variable | Obligatoria | Secreto | Uso / valor inicial |
|---|---|---|---|
| `MYSQL_DATABASE` | No | No | Base; default `red_clubes`. |
| `MYSQL_USER` | No | No | Usuario app no root; default `red_clubes_app`. |
| `MYSQL_PASSWORD` | Sí | Sí | App, Flyway y backup. Aleatoria y única. |
| `MYSQL_ROOT_PASSWORD` | Sí | Sí | Bootstrap/operación MySQL; Spring no la recibe. |
| `PUBLIC_ORIGIN` | Sí | No | Origen HTTPS exacto permitido por CORS. |
| `APP_PORT` | No | No | Puerto local; default `8080`, siempre ligado a loopback. |
| `TRUSTED_PROXY_CIDR` | Sí/revisar | No | IP/CIDR exacto del gateway/proxy; default `172.30.0.1/32`. |
| `DOCKER_SUBNET` / `DOCKER_GATEWAY` | No | No | Red interna coordinada; evitar colisiones. |
| `IMAGE_TAG` | Sí en release | No | Tag SemVer inmutable, igual para ambas imágenes. |
| `BACKEND_IMAGE` / `FRONTEND_IMAGE` | No | No | Nombre local o registry. |
| `AUTH_SESSION_HOURS` | No | No | Default 8. |
| `AUTH_LOGIN_MAX_ATTEMPTS` | No | No | Default 5. |
| `AUTH_LOGIN_WINDOW_MINUTES` | No | No | Default 15. |
| `AUTH_LOGIN_BLOCK_MINUTES` | No | No | Default 15. |
| `DB_POOL_MAX_SIZE` / `DB_POOL_MIN_IDLE` | No | No | Defaults 10/2; no elevar sin medir MySQL. |
| `DB_POOL_CONNECTION_TIMEOUT_MS` | No | No | Default 5000. |
| `DB_POOL_VALIDATION_TIMEOUT_MS` | No | No | Default 3000. |
| `DB_POOL_MAX_LIFETIME_MS` | No | No | Default 1800000. |
| `DEMO_DATA_ENABLED` | Sí/revisar | No | Debe ser `false`; los datos demo nunca crean usuarios. |
| `BOOTSTRAP_SUPERUSER_ENABLED` | Sí/revisar | No | `true` solo durante alta inicial; luego `false`. |
| `BOOTSTRAP_SUPERUSER_DNI/PASSWORD` | Solo bootstrap | Password sí | Eliminar después del aprovisionamiento. |
| `FLYWAY_BASELINE_ON_MIGRATE` | Sí/revisar | No | Debe ser `false`, salvo ensayo puntual aprobado. |
| `BACKUP_DIR` / `BACKUP_RETENTION_DAYS` | No | No | Destino y retención; default repo/backups y 14 días. |
| `BACKUP_EXPORT_HOOK` | Recomendado | No | Hook shell de cifrado/copia externa. |
| `BACKUP_AGE_RECIPIENT` | Opcional | No | Destinatario público `age`; la clave privada va fuera del host. |
| `BACKUP_COPY_DIR` | Opcional | No | Destino montado/sincronizado fuera del volumen de app. |

`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT` y `SPRING_PROFILES_ACTIVE` se usan en ejecución manual; Compose construye los equivalentes y fuerza perfil `prod`.

## Procedimiento inicial completo

### 1. Aprobar la versión

Partir de un tag SemVer cuyo commit tenga CI verde. Registrar tag, SHA, responsable y ventana. No desplegar `main`, ramas ni `latest` directamente.

### 2. Preparar el servidor

Usar Linux LTS x86_64/arm64 soportado por las imágenes, reloj/NTP activo y disco con alertas. Aplicar parches antes de cargar datos.

### 3. Crear acceso operativo

Crear un usuario sin login root directo, autenticar por clave SSH, desactivar contraseña SSH y restringir el origen de administración. Conceder solo el acceso necesario a Docker.

### 4. Configurar firewall

Permitir SSH desde IPs administrativas, HTTP 80 y HTTPS 443. No permitir 8080, 3306 ni el puerto backend desde Internet. Confirmar también reglas del proveedor cloud.

### 5. Instalar runtime

Instalar Docker Engine mantenido y Compose v2 desde el repositorio oficial del sistema. Verificar:

```bash
docker version
docker compose version
```

### 6. Obtener el release

```bash
sudo mkdir -p /opt/red-clubes
sudo chown "$USER" /opt/red-clubes
git clone <URL-REPOSITORIO> /opt/red-clubes
cd /opt/red-clubes
git fetch --tags --prune
git checkout v1.0.0
git status --short
```

El estado debe estar limpio y `git rev-parse HEAD` debe coincidir con el release.

### 7. Crear secretos y `.env`

```bash
cp .env.example .env
chmod 600 .env
```

Generar valores aleatorios distintos para `MYSQL_PASSWORD` y `MYSQL_ROOT_PASSWORD`; configurar `PUBLIC_ORIGIN=https://piloto.example.org`. Mantener `DEMO_DATA_ENABLED=false`, `FLYWAY_BASELINE_ON_MIGRATE=false` y `BOOTSTRAP_SUPERUSER_ENABLED=false`. No enviar `.env` por chat/correo ni incluirlo en backups del código.

### 8. Fijar red y proxy confiable

Los defaults coordinados son `DOCKER_SUBNET=172.30.0.0/24`, `DOCKER_GATEWAY=172.30.0.1` y `TRUSTED_PROXY_CIDR=172.30.0.1/32`. Verificar que no colisionen con redes VPN/host. Si se cambian, actualizar los tres. Nunca usar `0.0.0.0/0` como proxy confiable.

### 9. Fijar imágenes inmutables

Definir `IMAGE_TAG=v1.0.0`. Si existe registry, definir nombres completos en `BACKEND_IMAGE`/`FRONTEND_IMAGE` y ejecutar `docker compose pull`; si el host construye, usar:

```bash
docker compose build --pull
docker image inspect "${BACKEND_IMAGE:-red-clubes-backend}:${IMAGE_TAG}"
docker image inspect "${FRONTEND_IMAGE:-red-clubes-frontend}:${IMAGE_TAG}"
```

Registrar los digests resultantes.

### 10. Decidir base nueva o adopción

Para base nueva continuar con Flyway desde V1. Para datos preexistentes detenerse y completar `GUIA_MIGRACIONES.md` sobre una copia. Nunca habilitar baseline automáticamente en producción.

### 11. Validar configuración efectiva

```bash
docker compose config --quiet
docker compose config > /tmp/red-clubes-compose-effective.yml
```

Revisar que el único `ports` sea `127.0.0.1:APP_PORT:8080`, que MySQL/backend no publiquen puertos y que perfiles demo/bootstrap estén apagados. El archivo efectivo puede contener secretos: eliminarlo al terminar.

### 12. Iniciar MySQL

```bash
docker compose up -d mysql
docker compose ps
docker compose logs --tail=100 mysql
```

Esperar estado `healthy`. Verificar volumen, charset `utf8mb4`, zona horaria y espacio en disco.

### 13. Aplicar migraciones con backend

```bash
docker compose up -d backend
docker compose logs --tail=200 backend
curl --fail http://127.0.0.1:${APP_PORT:-8080}/api/health || true
```

Antes del frontend el curl puede no estar servido; la evidencia principal es el log Flyway: versión esperada, sin `repair` ni checksum alterado, Hibernate `validate` exitoso y contenedor `healthy`.

### 14. Iniciar frontend

```bash
docker compose up -d frontend
docker compose ps
curl --fail http://127.0.0.1:${APP_PORT:-8080}/healthz
curl --fail http://127.0.0.1:${APP_PORT:-8080}/api/health
```

### 15. Configurar DNS y TLS

Apuntar el dominio al servidor y usar Caddy, Nginx o Traefik mantenido en el host. Ejemplo Caddy:

```caddyfile
piloto.example.org {
    encode zstd gzip
    reverse_proxy 127.0.0.1:8080
}
```

El proxy debe obtener/renovar certificado y redirigir HTTP a HTTPS. Si se usa Nginx externo, sobrescribir `X-Forwarded-For` con `$remote_addr`, fijar `X-Forwarded-Proto $scheme` y no aceptar tráfico directo al puerto local.

### 16. Verificar headers e IP real

```bash
curl -I https://piloto.example.org/
curl -i https://piloto.example.org/api/health
curl -i -H 'X-Forwarded-For: 198.51.100.10' https://piloto.example.org/api/auth/login
```

Confirmar TLS válido, redirect 80→443, CSP/HSTS, ausencia de versión Nginx y que el proxy público no permita al cliente decidir la IP usada por el rate limit. Ajustar solo el CIDR exacto si la topología difiere.

### 17. Aprovisionar el primer usuario

Solo en instalación nueva, habilitar temporalmente `BOOTSTRAP_SUPERUSER_ENABLED=true`, DNI y contraseña inicial fuerte; recrear backend, iniciar sesión y cambiar contraseña. Luego deshabilitar bootstrap, eliminar esas variables del `.env` y recrear backend. Registrar quién realizó el alta, no la contraseña.

### 18. Ejecutar smoke test

```bash
BASE_URL=https://piloto.example.org REQUIRE_HTTPS=true sh ops/smoke-test.sh
```

Opcionalmente agregar `SMOKE_DNI` y `SMOKE_PASSWORD` de una cuenta de prueba sin datos reales. El script verifica frontend, ambos health, `401`, `404`, login/sesión opcionales y certificado HTTPS.

### 19. Programar backups

Destino local dedicado y retención inicial de 14 días:

```cron
15 3 * * * cd /opt/red-clubes && BACKUP_DIR=/var/backups/red-clubes BACKUP_RETENTION_DAYS=14 sh ops/backup-mysql.sh >> /var/log/red-clubes-backup.log 2>&1
```

Configurar `BACKUP_EXPORT_HOOK` para cifrado/copia externa. Probar el cron con el mismo usuario y alertar por fallo o `.last-success` antiguo.

### 20. Ensayar restauración

```bash
sh ops/restore-test.sh /var/backups/red-clubes/red-clubes-<fecha>.sql.gz
```

Debe crear tablas y leer `flyway_schema_history` en un MySQL temporal sin puertos. Registrar fecha, archivo, checksum, duración y conteos. No usar `restore-mysql.sh` para el ensayo.

### 21. Activar monitoreo

Configurar checks externos para `/healthz` y `/api/health`, alertas de disco/memoria/reinicios, vencimiento TLS y backup. Asignar destinatario y tiempo de respuesta según `OBSERVABILIDAD.md`.

### 22. Aceptar o abortar el go-live

Completar la checklist final, registrar release/digests, evidencia de CI, smoke, backup/restore, responsables y contacto de incidente. Si cualquier control bloqueante falla, mantener el servicio sin datos reales o revertir.

## Actualización segura

1. Crear release/tag e imágenes con el mismo SemVer; CI debe estar verde.
2. Leer migraciones entre versión actual y nueva. Ensayar con copia si transforman datos.
3. Ejecutar `ops/backup-mysql.sh`, validar checksum y exportación externa.
4. Descargar/construir imágenes nuevas sin reemplazar las anteriores.
5. Fijar `IMAGE_TAG` nuevo, ejecutar `docker compose config --quiet` y `docker compose up -d --wait`.
6. Observar Flyway, health, `5xx`, memoria y reinicios.
7. Ejecutar smoke con HTTPS y prueba funcional multi-club.
8. Registrar resultado; conservar imagen/tag anterior y backup según retención.

## Rollback

Si no hubo migración incompatible, restaurar el `IMAGE_TAG` anterior y ejecutar `docker compose up -d --wait`, luego smoke. No ejecutar `flyway repair`, no editar checksums y no intentar “deshacer” SQL manualmente.

Si el esquema dejó de ser compatible, el rollback es una recuperación de datos: detener escrituras/backend, conservar evidencia, verificar el backup externo y su restore aislado, y recién entonces ejecutar:

```bash
CONFIRM_RESTORE=RESTAURAR ALLOW_PRODUCTION_RESTORE=true \
  sh ops/restore-mysql.sh /ruta/al/backup.sql.gz
```

El script crea otro backup previo y deja backend detenido. Fijar el tag compatible, iniciar servicios, correr smoke y habilitar tráfico solo con aprobación. El RPO es la antigüedad del backup; el RTO debe medirse en simulacros.

## Releases e imágenes

- SemVer: patch para fixes compatibles, minor para capacidad compatible, major para cambios incompatibles.
- Tag Git firmado/anotado, release notes y changelog de migraciones.
- `IMAGE_TAG` idéntico para ambos componentes; registrar digest OCI.
- Nunca reconstruir/publicar un tag existente ni usar `latest` en producción.
- Conservar al menos versión actual/anterior y artefactos durante la ventana de rollback.
- Dependabot propone actualizaciones; cada una pasa las mismas pruebas y scanners.

## Migración futura a `ensenada.gov.ar`

Coordinar dominio, DNS, certificados, host, responsables, tratamiento de datos, retención e incidentes con el área municipal. Crear ambiente previo separado; probar las mismas imágenes/digests, exportar el último backup cifrado, verificar checksum, restaurar y comparar conteos. En el corte: congelar escrituras, backup final, importar, fijar `PUBLIC_ORIGIN`, secretos nuevos, DNS/TLS y smoke. Mantener el piloto intacto hasta aceptación municipal y vencimiento del rollback acordado.

## Checklist de salida

- [ ] Tag/SHA y digests inmutables registrados; CI completa verde.
- [ ] Host parchado, SSH endurecido y firewall verificado desde fuera.
- [ ] `.env` 0600, secretos únicos, demo/baseline/bootstrap apagados.
- [ ] Solo 80/443 públicos; 8080 loopback; MySQL/backend privados.
- [ ] TLS, redirect, headers, CSP e IP real comprobados.
- [ ] Flyway e Hibernate validate correctos; ningún migration checksum alterado.
- [ ] Smoke público y prueba de aislamiento multi-club aprobados.
- [ ] Backup cifrado externo reciente y restore aislado aprobado.
- [ ] Monitoreo/alertas con responsables y contacto de incidente.
- [ ] Rollback practicado, RPO/RTO registrados y decisión de go-live firmada.

Hasta completar esta lista, el estado correcto es **candidato técnico**, no “listo para datos reales”.
