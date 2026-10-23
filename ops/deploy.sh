#!/usr/bin/env bash
# Checkout del tag aprobado. No modifica checkout/.env ni hace rollback.
set -euo pipefail
umask 077
fail() { printf '%s\n' "$*" >&2; exit 1; }
[[ $# == 2 ]] || fail 'Uso: bash ops/deploy.sh v0.1.0-pilot build|pull|existing'
tag=$1
mode=$2
[[ $tag =~ ^v[0-9]+\.[0-9]+\.[0-9]+(-[A-Za-z0-9.-]+)?$ ]] || fail 'Se requiere tag versionado, nunca latest/ramas.'
[[ $mode == build || $mode == pull || $mode == existing ]] || fail 'Modo invalido'
cd "$(dirname "$0")/.."
for tool in git docker jq curl; do command -v "$tool" >/dev/null || fail "Falta $tool"; done
[[ -f .env && ! -L .env ]] || fail 'Se requiere .env regular local.'
[[ $(stat -c %a .env) == 600 && $(stat -c %u .env) == "$(id -u)" ]] || fail '.env debe pertenecer al operador y tener modo 600.'
[[ -z $(git status --porcelain) ]] || fail 'Checkout con cambios: guardar cambios antes de desplegar.'
git fetch origin --tags
revision=$(git rev-parse "refs/tags/$tag^{commit}")
[[ $(git rev-parse HEAD) == "$revision" ]] || fail 'HEAD no coincide con tag: checkout manual requerido.'
export IMAGE_TAG=$tag
work=$(mktemp -d)
trap 'rm -f -- "$work/config.json"; rmdir "$work"' EXIT
docker compose config --format json > "$work/config.json"
# No imprimir config: contiene secretos.
jq -e '.services.backend.environment |
  .SPRING_PROFILES_ACTIVE == "prod" and .FLYWAY_ENABLED == "true" and
  .FLYWAY_BASELINE_ON_MIGRATE == "false" and .DEMO_DATA_ENABLED == "false" and
  .BOOTSTRAP_SUPERUSER_ENABLED == "false" and
  (.CORS_ALLOWED_ORIGINS | startswith("https://")) and
  (.CORS_ALLOWED_ORIGINS | contains("example.com") | not)' "$work/config.json" >/dev/null || fail 'Revisar prod/Flyway/demo/bootstrap/PUBLIC_ORIGIN.'
jq -e '.services.mysql.environment |
  (.MYSQL_PASSWORD | length >= 32) and (.MYSQL_ROOT_PASSWORD | length >= 32) and
  (.MYSQL_PASSWORD != .MYSQL_ROOT_PASSWORD) and
  ([.MYSQL_PASSWORD,.MYSQL_ROOT_PASSWORD] | all(test("change_me|REPLACE_WITH";"i") | not))' "$work/config.json" >/dev/null || fail 'Configurar secretos MySQL diferentes y fuertes.'
jq -e '.services |
  ((.backend.ports // []) | length == 0) and ((.mysql.ports // []) | length == 0) and
  (.frontend.ports | length == 1 and all(.host_ip == "127.0.0.1"))' "$work/config.json" >/dev/null || fail 'Publicacion de puertos inesperada.'
persisted=$(env -u IMAGE_TAG docker compose config --format json | jq -r '[.services.backend.image,.services.frontend.image] | join("\n")')
backend=$(jq -r '.services.backend.image' "$work/config.json")
frontend=$(jq -r '.services.frontend.image' "$work/config.json")
[[ $persisted == "$(printf '%s\n%s' "$backend" "$frontend")" ]] || fail 'Fijar IMAGE_TAG en .env antes de continuar.'
port=$(jq -r '.services.frontend.ports[0].published' "$work/config.json")
if [[ $mode == build || $mode == pull ]]; then
  for img in "$backend" "$frontend"; do
    if docker image inspect "$img" >/dev/null 2>&1; then fail "Tag local ya existe ($img): usar existing o version nueva."; fi
  done
  if [[ $mode == build ]]; then docker compose build --pull backend frontend
  else docker compose pull backend frontend; fi
fi
docker image inspect "$backend" "$frontend" >/dev/null
# En actualizaciones, terminar backup y exportacion antes de reemplazar servicios.
if [[ -n $(docker compose ps --all --quiet backend) ]]; then
  [[ -n ${BACKUP_EXPORT_HOOK:-} ]] || fail 'Actualizacion requiere BACKUP_* exportado y hook externo.'
  sh ops/backup-mysql.sh
fi
if ! docker compose up -d --no-build --wait --wait-timeout 240; then
  fail 'Health fallo. Datos/imagenes conservados. Ver compose ps/logs y RESTORE_PRODUCCION.md; rollback manual.'
fi
BASE_URL="http://127.0.0.1:$port" REQUIRE_HTTPS=false sh ops/smoke-test.sh || fail 'Smoke fallo. Revisar logs/rollback manual. No se borraron volumenes ni imagenes.'
mkdir -p .deploy-records
record=".deploy-records/$tag-$(date -u +%Y%m%dT%H%M%SZ).txt"
{
  printf 'tag=%s\ncommit=%s\n' "$tag" "$revision"
  docker image inspect --format '{{.RepoTags}} {{.Id}} {{.RepoDigests}}' "$backend" "$frontend"
  docker inspect --format '{{.Name}} image={{.Image}} health={{.State.Health.Status}}' "$(docker compose ps -q mysql)"
} > "$record"
echo "Deploy local verificado. Evidencia: $record. Faltan HTTPS/IP real/checklist en servidor."
