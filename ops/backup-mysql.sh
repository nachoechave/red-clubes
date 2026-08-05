#!/usr/bin/env sh
set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
backup_dir=${BACKUP_DIR:-"$project_dir/backups"}
retention_days=${BACKUP_RETENTION_DAYS:-14}
timestamp=$(date -u +%Y%m%dT%H%M%SZ)
backup_file="$backup_dir/red-clubes-$timestamp.sql.gz"

mkdir -p "$backup_dir"
cd "$project_dir"

docker compose exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump --single-transaction --routines --triggers --set-gtid-purged=OFF --user=root "$MYSQL_DATABASE"' \
  | gzip -9 > "$backup_file"

gzip -t "$backup_file"
find "$backup_dir" -maxdepth 1 -type f -name 'red-clubes-*.sql.gz' -mtime "+$retention_days" -delete
printf '%s\n' "Backup verificado: $backup_file"
