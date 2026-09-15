#!/usr/bin/env sh
set -eu

if [ "$#" -ne 1 ] || [ "${CONFIRM_RESTORE:-}" != "RESTAURAR" ] || [ "${ALLOW_PRODUCTION_RESTORE:-}" != "true" ]; then
  printf '%s\n' 'Uso: CONFIRM_RESTORE=RESTAURAR ALLOW_PRODUCTION_RESTORE=true sh ops/restore-mysql.sh <backup.sql.gz>' >&2
  exit 2
fi

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
backup_file=$1
case "$backup_file" in
  /*) ;;
  *) backup_file="$project_dir/$backup_file" ;;
esac

if [ ! -f "$backup_file" ]; then
  printf '%s\n' "No existe el backup: $backup_file" >&2
  exit 2
fi

gzip -t "$backup_file"
if [ -f "$backup_file.sha256" ]; then
  backup_dir=$(dirname -- "$backup_file")
  checksum_name=$(basename -- "$backup_file.sha256")
  if command -v sha256sum >/dev/null 2>&1; then
    (cd "$backup_dir" && sha256sum -c "$checksum_name")
  elif command -v shasum >/dev/null 2>&1; then
    expected=$(cut -d ' ' -f 1 "$backup_file.sha256")
    actual=$(shasum -a 256 "$backup_file" | cut -d ' ' -f 1)
    [ "$expected" = "$actual" ] || { printf '%s\n' 'Checksum invalido.' >&2; exit 1; }
  else
    printf '%s\n' 'No se pudo validar el checksum: falta sha256sum/shasum.' >&2
    exit 1
  fi
fi

if ! gzip -dc "$backup_file" | grep -q '^CREATE TABLE'; then
  printf '%s\n' 'El backup no contiene sentencias CREATE TABLE.' >&2
  exit 1
fi

cd "$project_dir"
docker compose stop backend

if [ "${SKIP_PRE_RESTORE_BACKUP:-false}" != "true" ]; then
  sh "$project_dir/ops/backup-mysql.sh"
fi

gzip -dc "$backup_file" | docker compose exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_PASSWORD" mysql --user="$MYSQL_USER" "$MYSQL_DATABASE"'

printf '%s\n' 'Restauracion finalizada con backend detenido.'
printf '%s\n' 'Inicia backend/frontend y ejecuta ops/smoke-test.sh antes de habilitar trafico.'
