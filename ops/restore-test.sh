#!/usr/bin/env sh
set -eu

if [ "$#" -ne 1 ]; then
  printf '%s\n' 'Uso: sh ops/restore-test.sh <backup.sql.gz>' >&2
  exit 2
fi

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
backup_file=$1
case "$backup_file" in
  /*) ;;
  *) backup_file="$project_dir/$backup_file" ;;
esac
[ -f "$backup_file" ] || { printf '%s\n' "No existe el backup: $backup_file" >&2; exit 2; }
gzip -t "$backup_file"

timestamp=$(date -u +%Y%m%d%H%M%S)
container_name="red-clubes-restore-test-$timestamp-$$"
test_password="restore-test-$timestamp-$$"

cleanup() {
  docker rm -f "$container_name" >/dev/null 2>&1 || true
}
trap cleanup EXIT HUP INT TERM

docker run --detach --rm --name "$container_name" \
  --network none \
  --tmpfs /tmp:size=64m,mode=1777 \
  --env MYSQL_ROOT_PASSWORD="$test_password" \
  --env MYSQL_DATABASE=restore_test \
  mysql:8.4 >/dev/null

attempt=0
until docker exec "$container_name" mysqladmin ping --host=127.0.0.1 --user=root --password="$test_password" --silent >/dev/null 2>&1; do
  attempt=$((attempt + 1))
  if [ "$attempt" -ge 60 ]; then
    printf '%s\n' 'MySQL temporal no estuvo listo a tiempo.' >&2
    exit 1
  fi
  sleep 2
done

gzip -dc "$backup_file" | docker exec -i "$container_name" \
  mysql --user=root --password="$test_password" restore_test

table_count=$(docker exec "$container_name" mysql --batch --skip-column-names \
  --user=root --password="$test_password" \
  --execute="SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='restore_test';")
flyway_count=$(docker exec "$container_name" mysql --batch --skip-column-names \
  --user=root --password="$test_password" restore_test \
  --execute='SELECT COUNT(*) FROM flyway_schema_history;')

[ "$table_count" -gt 0 ] || { printf '%s\n' 'La restauracion no creo tablas.' >&2; exit 1; }
[ "$flyway_count" -gt 0 ] || { printf '%s\n' 'La restauracion no contiene historial Flyway.' >&2; exit 1; }

printf '%s\n' "Restore aislado verificado: $table_count tablas, $flyway_count migraciones registradas."
