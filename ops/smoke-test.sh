#!/usr/bin/env sh
set -eu
umask 077
base_url=${BASE_URL:-http://127.0.0.1:8080}
base_url=${base_url%/}
case "$base_url" in
  https://*) ;;
  http://*) [ "${REQUIRE_HTTPS:-auto}" != true ] || { echo 'REQUIRE_HTTPS=true exige HTTPS' >&2; exit 2; } ;;
  *) echo 'BASE_URL debe usar HTTP(S)' >&2; exit 2;;
esac
work=$(mktemp -d)
cleanup() { rm -f -- "$work/response" "$work/headers" "$work/login.json" "$work/auth.conf"; rmdir "$work"; }
trap cleanup EXIT HUP INT TERM
assert_status() {
  expected=$1; description=$2; shift 2
  actual=$(curl --silent --show-error --connect-timeout 5 --max-time 30 \
    --output "$work/response" --dump-header "$work/headers" --write-out '%{http_code}' "$@")
  [ "$actual" = "$expected" ] || { echo "$description: esperado $expected, recibido $actual" >&2; exit 1; }
  echo "OK - $description (HTTP $actual)"
}
assert_status 200 'frontend' "$base_url/"
grep -qi '^Content-Security-Policy:' "$work/headers" || { echo 'Falta CSP en HTML' >&2; exit 1; }
grep -qi '^X-Content-Type-Options: nosniff' "$work/headers" || { echo 'Falta nosniff en HTML' >&2; exit 1; }
grep -qi '^Cache-Control: no-store' "$work/headers" || { echo 'Falta no-store en HTML' >&2; exit 1; }
if grep -qi 'onload=' "$work/response"; then echo 'HTML usa handler inline incompatible con CSP' >&2; exit 1; fi
assert_status 200 'health frontend' "$base_url/healthz"
assert_status 200 'health backend/DB' "$base_url/api/health"
grep -q '"status":"UP"' "$work/response" || { echo 'Backend no reporta UP' >&2; exit 1; }
assert_status 401 'API protegida' "$base_url/api/clubes"
assert_status 404 'estatico inexistente' "$base_url/smoke-test-no-existe.js"
if [ -n "${SMOKE_DNI:-}" ] || [ -n "${SMOKE_PASSWORD:-}" ]; then
  [ -n "${SMOKE_DNI:-}" ] && [ -n "${SMOKE_PASSWORD:-}" ] || { echo 'Configurar ambas credenciales ficticias' >&2; exit 2; }
  command -v jq >/dev/null || { echo 'Auth smoke requiere jq' >&2; exit 2; }
  export SMOKE_DNI SMOKE_PASSWORD
  jq -n '{dni:env.SMOKE_DNI,password:env.SMOKE_PASSWORD}' > "$work/login.json"
  unset SMOKE_PASSWORD SMOKE_DNI
  assert_status 200 'login opcional' -H 'Content-Type: application/json' --data-binary "@$work/login.json" "$base_url/api/auth/login"
  token=$(jq -er '.token | select(type == "string" and test("^[A-Za-z0-9_-]+$"))' "$work/response")
  printf 'header = "Authorization: Bearer %s"\n' "$token" > "$work/auth.conf"
  unset token
  assert_status 200 'sesion' --config "$work/auth.conf" "$base_url/api/auth/me"
  assert_status 200 'logout' --config "$work/auth.conf" -X POST "$base_url/api/auth/logout"
  assert_status 401 'token revocado' --config "$work/auth.conf" "$base_url/api/auth/me"
fi
case "$base_url" in https://*) echo 'OK - certificado validado por curl';; *) echo 'PENDIENTE - HTTPS en dominio publico';; esac
echo 'Smoke completado.'
