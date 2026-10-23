# Primer piloto en Cloud Server Linux

Estado: **candidato para piloto controlado con datos ficticios**. No habilita datos personales reales. Partir del tag aprobado posterior al merge de este PR, con CI verde. `v0.1.0-pilot` es un EJEMPLO, no un tag ya publicado. Una sola instancia backend.

## 1. Arquitectura y recursos

```text
Internet 80/443 → Nginx HOST (TLS)
                 → 127.0.0.1:APP_PORT → frontend Nginx (edge + private)
                                      → backend Spring (private)
                                         → MySQL + volumen (private)
```

Backend/MySQL sin puertos host. `private` es interna; `edge` permite publicar solo frontend. No compartir redes/volúmenes con ComercioFlex u otra aplicación. Si el host ya tiene Nginx, agregar un virtual host dedicado, sin reemplazar sus sitios. Seleccionar APP_PORT libre y subred sin colisiones. No se requiere registry para el primer piloto.

Mínimo inicial: Linux LTS mantenido, 2 vCPU, 4 GB RAM y 30 GB SSD más espacio para imágenes/backups y restore temporal. Medir RAM extra durante compilación/restore; no ejecutar ambos simultáneamente en host pequeño. Si no alcanza, construir/ensayar fuera y transferir imágenes. No equivale a capacidad de usuarios certificada.

## 2. Usuario, sistema y SSH

Ejemplos para Ubuntu/Debian con systemd. Ejecutar instalación de paquetes/servicios como administrador; operación de aplicación como usuario dedicado `redclubes`.

```bash
sudo adduser --disabled-password --gecos "" redclubes
sudo install -d -o redclubes -g redclubes -m 0750 /opt/red-clubes
sudo install -d -o redclubes -g redclubes -m 0700 /var/backups/red-clubes
sudo install -d -o root -g redclubes -m 0750 /etc/red-clubes
sudo apt-get update
sudo apt-get install -y git curl jq openssl dnsutils nginx certbot age
timedatectl status
df -h
df -i
free -h
```

Instalar Docker Engine y Compose v2 según [guía oficial Ubuntu](https://docs.docker.com/engine/install/ubuntu/) (o la variante de la distribución). No cambiar Docker de un servidor compartido sin revisar sus aplicaciones. Verificar `docker version`, `docker compose version` y `sudo systemctl enable --now docker nginx`.

Agregar `redclubes` a `docker` y abrir una nueva sesión. **Pertenecer a docker equivale a control administrativo del host**; no es una sandbox. Mantener pocas cuentas, claves SSH y acceso auditado. Usar el usuario de administración para sudo; el de despliegue no necesita sudo habitual.

Instalar la clave pública autorizada con permisos `~/.ssh=700`, `authorized_keys=600`. Antes de deshabilitar login root/contraseñas SSH: probar una segunda sesión por clave y tener consola de recuperación del proveedor. Comprobar `sudo sshd -t`, aplicar cambios y mantener abierta la sesión original hasta verificar acceso nuevo. No ejecutar un reboot todavía.

## 3. Firewall

Comprobar primero puerto/origen reales SSH (`sudo ss -lntp`). Si SSH no usa 22, adaptar todas las reglas y el firewall cloud. Política de ejemplo, revisar IP administrativa:

```bash
sudo ufw status verbose
sudo ufw allow from IP_ADMINISTRATIVA to any port 22 proto tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw default deny incoming
sudo ufw default allow outgoing
# Solo tras validar acceso por clave, regla SSH y consola de recuperacion:
sudo ufw enable
sudo ufw status numbered
sudo ss -lntp
docker ps --format 'table {{.Names}}\t{{.Ports}}'
```

La única entrada general debe ser 80/443; 22 restringido a administración. Verificar IPv6 y firewall del proveedor. Nunca abrir 3306/8080/backend. Docker puede eludir reglas ufw para puertos publicados: el bind loopback y la ausencia de puertos DB/backend son obligatorios. Desde otro equipo comprobar que APP_PORT/3306 no conectan, no basta con leer ufw. Usar Docker mantenido; versiones anteriores a 28 tienen una advertencia oficial sobre puertos loopback accesibles desde el segmento L2.

## 4. DNS

Elegir dominio real; todos los archivos usan `redclubes.example.com`. Crear registro **A** del nombre `redclubes` hacia IPv4 pública. Crear **AAAA** solo si IPv6 funciona extremo a extremo (firewall, rutas y Nginx escuchando IPv6). No publicar AAAA roto. Revisar CAA si la zona restringe emisores.

```bash
dig +short A redclubes.example.com
dig +short AAAA redclubes.example.com
dig @1.1.1.1 +short A redclubes.example.com
dig @8.8.8.8 +short A redclubes.example.com
nslookup redclubes.example.com
```

Comparar con IPs del proveedor y con el nameserver autoritativo; esperar TTL, repetir desde otra red. DNS correcto no demuestra HTTPS ni aplicación.

## 5. Checkout y secretos

Como redclubes:

```bash
git clone https://github.com/nachoechave/red-clubes.git /opt/red-clubes
cd /opt/red-clubes
git fetch origin --tags
git checkout --detach v0.1.0-pilot
git rev-parse HEAD
git status --short
umask 077
cp .env.production.example .env
chmod 600 .env
```

El responsable debe haber creado un tag anotado sobre el commit aprobado y publicado ese tag. Verificar SHA contra release/CI, no confiar solo en el nombre. Editar `.env` local con editor seguro. Generar dos valores distintos con `openssl rand -hex 32`; generarlos solo en el servidor/gestor de secretos, nunca en Git ni en esta tarea. El formato hex evita interpolación de `$`/comillas en Compose. No pegar valores en argumentos shell ni tickets; no usar `set -x`. Salida de `docker compose config`/`docker inspect` completo puede revelar secretos: usar `config --quiet` y formatos selectivos.

| Configuración | Valor/decisión requerida |
|---|---|
| MYSQL_DATABASE, MYSQL_USER | Nombres de base y usuario no root; conservar entre releases |
| MYSQL_PASSWORD, MYSQL_ROOT_PASSWORD | Aleatorios, distintos, privados |
| PUBLIC_ORIGIN | HTTPS exacto del subdominio, sin slash final |
| APP_PORT | Puerto libre del host, loopback |
| TRUSTED_PROXY_CIDR | Peer medido, ver [prueba IP real](PROXY_Y_RATE_LIMIT.md) |
| DOCKER_SUBNET, DOCKER_GATEWAY | Solo red private; coordinar sin colisiones, no deducir proxy de aquí |
| BACKEND_IMAGE, FRONTEND_IMAGE, IMAGE_TAG | Nombres y tag inmutable del release |
| AUTH_SESSION_HOURS | 8 por defecto |
| AUTH_LOGIN_MAX_ATTEMPTS/WINDOW_MINUTES/BLOCK_MINUTES | 5/15/15 por defecto |
| BOOTSTRAP_SUPERUSER_ENABLED/DNI/PASSWORD/NAME/LASTNAME | Nombres reales; enabled=false salvo alta puntual |
| DEMO_DATA_ENABLED | false; crear datos ficticios con checklist |
| DB_POOL_* | Defaults acotados del ejemplo |
| BACKUP_DIR/RETENTION_DAYS/EXPORT_HOOK | En archivo operacional de systemd, ver backup |
| BACKUP_AGE_RECIPIENT/BACKUP_COPY_DIR | Destinatario público y montaje offsite |

`SUPERUSER_BOOTSTRAP_*` **no existe**. En Compose no definir DB_URL/DB_USERNAME/DB_PASSWORD por separado: se construyen con hostname `mysql`, MYSQL_USER y MYSQL_PASSWORD. Para Java manual sí existen esas tres variables, pero ese modo queda fuera del piloto. Compose fuerza prod, Flyway enabled y baseline=false; colocar otro valor en .env no lo sobrescribe. Las variables BACKUP_* en .env tampoco se exportan solas a scripts del host.

Rotación: cambiar contraseñas de cuentas desde aplicación; para MySQL, el entorno **no cambia passwords de un volumen existente**. Rotar con SQL administrativo por canal seguro y sesión interactiva, actualizar .env coordinadamente, recrear servicios y probar login/backup. Nunca borrar volumen para rotar. Revocar credenciales expuestas y sesiones relacionadas. Para age, incorporar el nuevo destinatario a backups futuros y conservar acceso a claves anteriores mientras haya backups retenidos.

## 6. Desplegar instalación nueva

Completar `.env`, dejar bootstrap apagado. Ejecutar:

```bash
docker compose config --quiet
bash ops/deploy.sh v0.1.0-pilot build
docker compose ps
curl --fail --max-time 10 http://127.0.0.1:8080/api/health
```

Reemplazar 8080 si APP_PORT cambia. El script comprueba tag/HEAD, permisos, secretos mínimos y puertos; construye sin sobrescribir tags existentes, espera health y ejecuta smoke. No automatiza checkout, rollback, bootstrap ni DNS/TLS. El modo `existing` reutiliza imágenes ya construidas; `pull` sirve solo si hay un registry propio autorizado y con tags inmutables. En actualizaciones exige backup con hook exportado antes de recrear. Registra commit/IDs/digests en `.deploy-records/`; copiar esa evidencia al registro de release.

## 7. Flyway en DB vacía

Mantener `DEMO_DATA_ENABLED=false`. Arranque nuevo crea V1 → V6; Hibernate valida. Comprobar:

```bash
docker compose logs --since=15m --tail=200 backend
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql --user="$MYSQL_USER" "$MYSQL_DATABASE" --execute="SELECT installed_rank,version,description,type,checksum,success FROM flyway_schema_history ORDER BY installed_rank; SHOW TABLES;"'
```

Esperar seis migraciones SQL exitosas, ninguna fila BASELINE para instalación nueva, versión final 6 y health UP. La lista de archivos `backend/src/main/resources/db/migration/` es autoridad si una release futura agrega V7+. Verificar esquema contra migraciones, nunca editar V1–V6 ni usar repair para ocultar errores. Si hay tablas previas sin historial: abortar y seguir [base histórica](MIGRACION_BASE_HISTORICA.md).

## 8. Certbot y Nginx HOST

Ejemplo webroot: no requiere que la app esté expuesta durante emisión.

1. Copiar `ops/nginx/red-clubes-http.conf.example` a `/etc/nginx/sites-available/red-clubes.conf`, sustituir dominio, habilitar symlink en sites-enabled (sin duplicados). Crear `/var/www/letsencrypt/.well-known/acme-challenge` con permisos de lectura Nginx. `sudo nginx -t && sudo systemctl reload nginx`.
2. Desde otra red comprobar HTTP y DNS. Emitir:

```bash
sudo certbot certonly --webroot -w /var/www/letsencrypt \
  --cert-name redclubes.example.com -d redclubes.example.com
```

Responder email/términos como operador. Los certificados quedan en /etc/letsencrypt, nunca en repo ni imagen. Si falla, comprobar DNS, AAAA, puerto 80 y CAA, no desactivar TLS.

3. Reemplazar el sitio por `ops/nginx/red-clubes.conf.example`; adaptar dominio, rutas de cert y puerto literal 8080. Nginx no expande ${APP_PORT}. Activar listens IPv6 solo si corresponde. `sudo nginx -t && sudo systemctl reload nginx`.
4. Crear hook local root-owned `/etc/letsencrypt/renewal-hooks/deploy/reload-nginx` (0755):

```sh
#!/bin/sh
set -eu
/usr/sbin/nginx -t
/bin/systemctl reload nginx
```

5. Para Certbot del paquete apt: `sudo systemctl enable --now certbot.timer`, `systemctl list-timers certbot.timer`, `sudo certbot renew --dry-run`. Si la instalación usa otro scheduler (snap/cron), comprobar ese mecanismo y evitar duplicarlo. Probar también el hook con `sudo /etc/letsencrypt/renewal-hooks/deploy/reload-nginx`; dry-run no necesariamente ejecuta deploy hooks.
6. Comprobar:

```bash
curl -I http://redclubes.example.com/
curl -I https://redclubes.example.com/
BASE_URL=https://redclubes.example.com REQUIRE_HTTPS=true sh ops/smoke-test.sh
sudo certbot certificates
```

HTTP debe redirigir 308 a HTTPS, certificado válido, TLS 1.2/1.3, headers CSP/HSTS/nosniff y no-store en HTML. Verificar assets y login en navegador sin errores CSP. No usar curl -k. Headers de aplicación vienen de Nginx interno; errores propios del host pueden diferir. Establecer [IP real y prueba anti-spoof](PROXY_Y_RATE_LIMIT.md) antes de aceptar el piloto.

## 9. Primer superadministrador

1. En .env privado: `BOOTSTRAP_SUPERUSER_ENABLED=true`, identificador sintético `9900000000` (diez dígitos, no DNI de una persona), password fuerte generado localmente y nombre de prueba. No usar passwords placeholder.
2. `docker compose up -d --no-deps --force-recreate --wait backend`. Por HTTPS iniciar sesión, comprobar rol SUPERUSUARIO y cambiar contraseña inicial.
3. Si el DNI ya existe, initializer retorna sin modificar rol, estado ni password. No es recuperación de cuenta. Usar otro identificador nuevo o recuperar mediante un superusuario válido; no resetear hashes manualmente.
4. `docker compose stop backend`. Cambiar enabled=false, **eliminar líneas DNI y PASSWORD** de .env (también limpiar variables exportadas del shell si se usaron).
5. `docker compose up -d --no-deps --force-recreate --wait backend`. Esto elimina credenciales del entorno del nuevo contenedor. Comprobar por HTTPS login con nueva contraseña, sesión, logout y rechazo de contraseña inicial.
6. Verificar de forma selectiva, sin mostrar valores: `docker compose exec -T backend sh -c 'test "$BOOTSTRAP_SUPERUSER_ENABLED" = false && test -z "$BOOTSTRAP_SUPERUSER_PASSWORD" && test -z "$BOOTSTRAP_SUPERUSER_DNI"'`.

No subir dumps de usuarios. Eliminar initializer demo del código no elimina cuentas antiguas en una DB reutilizada. Usar [procedimiento histórico](MIGRACION_BASE_HISTORICA.md).

## 10. Backup, monitoreo y reboot

Completar [backup diario cifrado](BACKUP_PILOTO.md), [restore](RESTORE_PRODUCCION.md) y [checklist ficticia](CHECKLIST_PILOTO.md).

Monitoreo mínimo: ejecutar `sh ops/check-pilot.sh https://redclubes.example.com` con BACKUP_DIR exportado, por timer/cron cada 5 min y registro privado. Retorna error en health, contenedores, disco, backup antiguo o certificado próximo a vencer. Un operador debe revisar fallos con una frecuencia acordada; conectar el aviso al monitor del proveedor, no considerar un log como notificación entregada. Repetir health desde otra red, porque desde el host no prueba Internet.

Ejemplo en `crontab -e` del usuario redclubes (adaptar dominio). `logger` conserva el resultado en journal; no es un servicio de alertas. Antes, probar manualmente el comando con el entorno de cron. Acordar quién revisa/recibe el fallo y probar un fallo deliberado sin afectar la app:

```cron
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
*/5 * * * * cd /opt/red-clubes && BACKUP_DIR=/var/backups/red-clubes sh ops/check-pilot.sh https://redclubes.example.com 2>&1 | logger -t red-clubes-monitor
```

El pipe de cron no conserva el exit code del check; un monitor del proveedor debe ejecutar el script directamente y evaluar su código. Para diagnóstico: `journalctl -t red-clubes-monitor --since today`, `journalctl -u red-clubes-backup.service --since today` y `systemctl --failed`.

### Logging y permisos

`docker compose logs --since=30m --tail=100 backend frontend mysql` permite diagnóstico acotado. Compose ya limita logs `json-file` con `max-size`/`max-file`; no agregar otro stack. Nginx interno/host no registra query strings ni Authorization/body en su access log de ejemplo. La aplicación no contiene logging explícito de passwords, tokens o hashes; `show-sql=false` en prod. Esto no garantiza que excepciones de librerías nunca incluyan valores: no habilitar DEBUG/TRACE ni publicar logs sin revisión/redacción. Las rutas pueden contener IDs. Mantener acceso a logs solo para operadores autorizados y retención mínima; revisar también logrotate de `/var/log/nginx` y límites de journald del host. `.env=600`, backups/directorios privados=700; configuración operacional `/etc/red-clubes/backup.env=640 root:redclubes`, scripts versionados sin secretos, no world-writable. Evitar historial de comandos con secretos (editor/gestor seguro; no argumentos ni `set -x`).

En ventana aprobada y con consola de recuperación, probar `sudo reboot`. Después:

```bash
systemctl is-active docker nginx
systemctl list-timers red-clubes-backup.timer certbot.timer
cd /opt/red-clubes
docker compose ps
docker compose logs --since=10m --tail=100 backend mysql
BASE_URL=https://redclubes.example.com REQUIRE_HTTPS=true sh ops/smoke-test.sh
```

`restart: unless-stopped` recupera contenedores que estaban corriendo; los detenidos manualmente no se reanudan. El daemon no vuelve a evaluar depends_on como un despliegue Compose: backend puede fallar primero y reintentarse hasta DB disponible. Validar recuperación real, sin prometer orden garantizado; si no vuelve solo, investigar logs y registrar pendiente. Revisar CIDR observado después de recreación de redes.

## 11. Releases, actualización y rollback

Tags inmutables de aplicación, nunca latest. Aunque el tag Git sea igual, rebuild con bases Docker móviles/apk puede producir bytes distintos: **no reconstruir un tag publicado**. Registrar image IDs/RepoDigests (un build local puede no tener RepoDigest). Guardar ambas imágenes con `docker image save ... | gzip > /ruta/privada/release-images.tar.gz`, checksum y almacenamiento externo; registrar también ID de MySQL. Para traslado usar docker load y verificar IDs. CI verde certifica su construcción, no automáticamente bytes reconstruidos en otro host: escanear artefactos destino con los mismos umbrales antes de abrir el piloto.

Para actualización: backup/export/restore de ensayo, leer migraciones, guardar evidencia anterior, fetch y checkout del nuevo tag, actualizar IMAGE_TAG en .env, exportar BACKUP_* desde configuración confiable (ver backup), ejecutar deploy build/pull/existing. Probar HTTPS, IP real y circuitos ficticios.

Rollback de aplicación (solo si DB compatible):
1. Mantener ventana de mantenimiento y pausar timer backup durante cambio de checkout.
2. Recuperar imágenes anteriores conservadas; verificar IDs contra registro.
3. Checkout del tag anterior y fijar IMAGE_TAG anterior en .env; conservar secretos/topología vigentes.
4. `docker compose up -d --no-build --pull never --wait --wait-timeout 240`.
5. Smoke local/HTTPS, login/aislamiento y versión; reanudar timer y registrar resultado.

Si hubo migración incompatible/destructiva, no intentar revertir Flyway ni arrancar binario incompatible: seguir RESTORE_PRODUCCION, con aprobación y backup de la versión anterior. Recuperar primero DB compatible y luego imagen; aceptar pérdida de escrituras desde el backup (RPO) solo con responsable.

## Fuentes operativas

[Docker publicación de puertos](https://docs.docker.com/engine/network/port-publishing/), [Certbot emisión/renovación](https://eff-certbot.readthedocs.io/en/stable/using.html), [Nginx headers](https://nginx.org/en/docs/http/ngx_http_headers_module.html). Revisar instrucciones de la distribución elegida al instalar.
