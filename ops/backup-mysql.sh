#!/usr/bin/env sh
set -eu

umask 077

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
backup_dir=${BACKUP_DIR:-"$project_dir/backups"}
retention_days=${BACKUP_RETENTION_DAYS:-14}
timestamp=$(date -u +%Y%m%dT%H%M%SZ)
backup_file="$backup_dir/red-clubes-$timestamp.sql.gz"
checksum_file="$backup_file.sha256"
temporary_sql="$backup_dir/.red-clubes-$timestamp.sql.tmp"
temporary_gzip="$backup_dir/.red-clubes-$timestamp.sql.gz.tmp"

case "$backup_dir" in
  ""|/) printf '%s\n' 'BACKUP_DIR no puede estar vacio ni ser /.' >&2; exit 2 ;;
esac
case "$retention_days" in
  ''|*[!0-9]*) printf '%s\n' 'BACKUP_RETENTION_DAYS debe ser un entero no negativo.' >&2; exit 2 ;;
esac

cleanup() {
  rm -f -- "$temporary_sql" "$temporary_gzip" "$checksum_file.tmp" "$backup_dir/.last-success.tmp"
}
trap cleanup EXIT HUP INT TERM

mkdir -p "$backup_dir"
cd "$project_dir"

docker compose exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_PASSWORD" mysqldump --single-transaction --quick --routines --triggers --events --hex-blob --no-tablespaces --set-gtid-purged=OFF --user="$MYSQL_USER" "$MYSQL_DATABASE"' \
  > "$temporary_sql"

if [ ! -s "$temporary_sql" ] || ! grep -q '^CREATE TABLE' "$temporary_sql"; then
  printf '%s\n' 'El dump esta vacio o no contiene tablas; no se publico ningun backup.' >&2
  exit 1
fi

gzip -9 < "$temporary_sql" > "$temporary_gzip"
gzip -t "$temporary_gzip"
mv -- "$temporary_gzip" "$backup_file"

if command -v sha256sum >/dev/null 2>&1; then
  (cd "$backup_dir" && sha256sum "$(basename "$backup_file")") > "$checksum_file.tmp"
elif command -v shasum >/dev/null 2>&1; then
  (cd "$backup_dir" && shasum -a 256 "$(basename "$backup_file")") > "$checksum_file.tmp"
else
  printf '%s\n' 'No se encontro sha256sum ni shasum para generar la suma de control.' >&2
  exit 1
fi
mv -- "$checksum_file.tmp" "$checksum_file"

printf '%s\n' "$(basename "$backup_file")" > "$backup_dir/.last-success.tmp"
mv -- "$backup_dir/.last-success.tmp" "$backup_dir/.last-success"

find "$backup_dir" -maxdepth 1 -type f \( -name 'red-clubes-*.sql.gz' -o -name 'red-clubes-*.sql.gz.sha256' \) \
  -mtime "+$retention_days" -delete

if [ -n "${BACKUP_EXPORT_HOOK:-}" ]; then
  if [ ! -f "$BACKUP_EXPORT_HOOK" ]; then
    printf '%s\n' "No existe BACKUP_EXPORT_HOOK: $BACKUP_EXPORT_HOOK" >&2
    exit 1
  fi
  sh "$BACKUP_EXPORT_HOOK" "$backup_file"
fi

rm -f -- "$temporary_sql"
trap - EXIT HUP INT TERM
printf '%s\n' "Backup atomico y verificado: $backup_file"
