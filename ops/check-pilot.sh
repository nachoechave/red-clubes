#!/usr/bin/env sh
# Monitor sin reparaciones automaticas; requiere GNU stat/df, openssl y timeout.
set -eu
[ "$#" -eq 1 ] || { echo 'Uso: check-pilot.sh https://redclubes.DOMINIO' >&2; exit 2; }
origin=${1%/}
case "$origin" in https://*) ;; *) echo 'Se requiere origen HTTPS' >&2; exit 2;; esac
host=${origin#https://}
case "$host" in ''|*/*|*:*|*@*|*[!a-zA-Z0-9.-]*) echo 'Usar hostname HTTPS sin path/puerto' >&2; exit 2;; esac
cd "$(dirname "$0")/.."
: "${BACKUP_DIR:?Exportar BACKUP_DIR de configuracion operativa}"
for service in mysql backend frontend; do
  container=$(docker compose ps --quiet "$service")
  [ -n "$container" ] || { echo "$service detenido" >&2; exit 1; }
  state=$(docker inspect --format '{{.State.Health.Status}}' "$container")
  [ "$state" = healthy ] || { echo "$service no saludable" >&2; exit 1; }
done
curl --fail --silent --show-error --connect-timeout 5 --max-time 15 "$origin/healthz" >/dev/null
curl --fail --silent --show-error --connect-timeout 5 --max-time 15 "$origin/api/health" | grep -q '"status":"UP"'
for directory in / /var/lib/docker "$BACKUP_DIR"; do
  used=$(df -P "$directory" | awk 'NR==2 {gsub(/%/,"",$5); print $5}')
  [ -n "$used" ] && [ "$used" -lt 80 ] || { echo "Disco >=80%: $directory" >&2; exit 1; }
done
[ -f "$BACKUP_DIR/.last-success" ] || { echo 'Sin backup exitoso' >&2; exit 1; }
age_seconds=$(( $(date +%s) - $(stat -c %Y "$BACKUP_DIR/.last-success") ))
[ "$age_seconds" -ge 0 ] && [ "$age_seconds" -lt 93600 ] || { echo 'Backup de mas de 26h o reloj incorrecto' >&2; exit 1; }
name=$(cat "$BACKUP_DIR/.last-success")
case "$name" in red-clubes-*.sql.gz) ;; *) echo 'Marcador invalido' >&2; exit 1;; esac
case "$name" in */*) exit 1;; esac
[ -s "$BACKUP_DIR/$name" ] && [ -f "$BACKUP_DIR/$name.sha256" ] || { echo 'Backup marcado no existe' >&2; exit 1; }
# Curl ya valido cadena/hostname. Aqui comprobar horizonte de 21 dias.
timeout 15 openssl s_client -connect "$host:443" -servername "$host" </dev/null 2>/dev/null \
  | openssl x509 -noout -checkend 1814400 >/dev/null
echo 'OK health, contenedores, disco, backup <26h, TLS >21d'
