#!/usr/bin/env sh
set -eu

base_url=${BASE_URL:-http://127.0.0.1:8080}
base_url=${base_url%/}
require_https=${REQUIRE_HTTPS:-auto}
response_file=$(mktemp)

cleanup() {
  rm -f -- "$response_file"
}
trap cleanup EXIT HUP INT TERM

request_status() {
  curl --silent --show-error --output "$response_file" --write-out '%{http_code}' "$@"
}

assert_status() {
  expected=$1
  description=$2
  shift 2
  actual=$(request_status "$@")
  if [ "$actual" != "$expected" ]; then
    printf '%s\n' "$description: se esperaba HTTP $expected y se obtuvo $actual" >&2
    exit 1
  fi
  printf '%s\n' "OK - $description (HTTP $actual)"
}

assert_status 200 'frontend accesible' "$base_url/"
assert_status 200 'health de frontend' "$base_url/healthz"
assert_status 200 'health de backend y base' "$base_url/api/health"
grep -q '"status":"UP"' "$response_file" || { printf '%s\n' 'Health backend no reporto UP.' >&2; exit 1; }
assert_status 401 'endpoint protegido sin sesion' "$base_url/api/clubes"
assert_status 404 'recurso estatico inexistente' "$base_url/smoke-test-no-existe.js"

if [ -n "${SMOKE_DNI:-}" ] || [ -n "${SMOKE_PASSWORD:-}" ]; then
  [ -n "${SMOKE_DNI:-}" ] && [ -n "${SMOKE_PASSWORD:-}" ] || {
    printf '%s\n' 'SMOKE_DNI y SMOKE_PASSWORD deben configurarse juntos.' >&2
    exit 2
  }
  login_payload=$(printf '{"dni":"%s","password":"%s"}' "$SMOKE_DNI" "$SMOKE_PASSWORD")
  assert_status 200 'login con credenciales opcionales' \
    --header 'Content-Type: application/json' --data "$login_payload" "$base_url/api/auth/login"
  token=$(sed -n 's/.*"token":"\([^"]*\)".*/\1/p' "$response_file")
  [ -n "$token" ] || { printf '%s\n' 'El login no devolvio token.' >&2; exit 1; }
  assert_status 200 'sesion autenticada' --header "Authorization: Bearer $token" "$base_url/api/auth/me"
fi

case "$base_url" in
  https://*) printf '%s\n' 'OK - HTTPS activo y certificado validado por curl' ;;
  http://*)
    if [ "$require_https" = "true" ]; then
      printf '%s\n' 'REQUIRE_HTTPS=true pero BASE_URL usa HTTP.' >&2
      exit 1
    fi
    printf '%s\n' 'OMITIDO - HTTPS no aplica al BASE_URL local; validarlo en el dominio publico.'
    ;;
  *) printf '%s\n' 'BASE_URL debe comenzar con http:// o https://.' >&2; exit 2 ;;
esac

printf '%s\n' 'Smoke test completado.'
