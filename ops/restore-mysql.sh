#!/usr/bin/env sh
set -eu

if [ "$#" -ne 1 ] || [ "${CONFIRM_RESTORE:-}" != "RESTAURAR" ]; then
  printf '%s\n' 'Uso: CONFIRM_RESTORE=RESTAURAR sh ops/restore-mysql.sh <backup.sql.gz>' >&2
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
cd "$project_dir"
gzip -dc "$backup_file" | docker compose exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --user=root "$MYSQL_DATABASE"'

printf '%s\n' 'Restauracion finalizada. Ejecuta los smoke tests antes de habilitar trafico.'
