# IP real y rate limiting en el piloto

Red Clubes piloto soporta **una sola instancia backend**. El contador vive en memoria y se reinicia con el proceso; no hay Redis. `LoginAttemptService` combina DNI normalizado + `HttpServletRequest.getRemoteAddr()`; con defaults bloquea tras 5 fallos, ventana 15 min, bloqueo 15 min. Responde 401 genérico incluso durante bloqueo. Nginx limita por IP a 10 solicitudes/minuto con burst 5 y responde 429. Son controles diferentes.

## Frontera de confianza

| Salto | Dirección / tratamiento |
|---|---|
| Dispositivo → Nginx HOST | `$remote_addr` es la IP vista por el servidor; con NAT varios usuarios pueden compartirla. El ejemplo sobrescribe X-Forwarded-For y X-Real-IP, fija Host y X-Forwarded-Proto, elimina Forwarded. |
| Host → frontend Docker | El peer puede ser un gateway de `edge` o una dirección del host según Docker/Linux. `private` tiene gateway fijo, `edge` no. **Medir**, no copiar `DOCKER_GATEWAY`. |
| Frontend → Spring | Peer TCP es frontend en `private`. Nginx envía X-Forwarded-For/X-Real-IP = `$remote_addr` normalizado. Spring usa `forward-headers-strategy=framework`, y el controlador pasa esa IP al rate limiter. |

No introducir un CDN/proxy adicional durante el piloto. Si existe, este ejemplo debe revisarse para sus IPs verificadas; no confiar en headers del cliente ni usar `0.0.0.0/0`, `::/0` o una subred completa por comodidad.

## Medición en destino (bloqueante)

1. Levantar frontend con `TRUSTED_PROXY_CIDR=127.0.0.1/32` inicialmente. Instalar Nginx host/HTTPS. Registrar la hora UTC.
2. Desde OTRO dispositivo/red hacer `curl -i https://redclubes.example.com/api/health`. El health no contiene datos personales. Repetir con `-H 'X-Forwarded-For: 198.51.100.77' -H 'X-Real-IP: 198.51.100.77' -H 'Forwarded: for=198.51.100.77'`.
3. En el servidor: `docker compose logs --since=2m --tail=100 frontend`. El formato muestra `peer=... client=... uri=/api/health`; correlacionar con `/var/log/nginx/red-clubes-access.log`. No usar DEBUG ni volcar headers/body. Inicialmente `client` puede ser el gateway.
4. Verificar que `peer` corresponde al host/gateway en `docker network inspect red-clubes_edge red-clubes_private`. Configurar **esa IP exacta /32 (IPv4) o /128 (IPv6)** en `.env` como `TRUSTED_PROXY_CIDR`.
5. `docker compose up -d --no-deps --force-recreate --wait frontend`. Repetir ambas peticiones. Ahora `client` debe coincidir con el cliente observado por el host, y nunca con la IP falsa. El backend recibe ese valor porque Nginx sobrescribe ambos headers.
6. Repetir después de reboot/recrear redes. Si cambió el gateway, corregir CIDR y repetir. No ampliar la confianza para esconder el problema. Registrar peer, client y resultado (sin tokens) en checklist.

## Prueba observable del contador de backend

Usar una cuenta **ficticia** activa (`9900000090`, nombre PILOTO RATE), password cambiado y comprobado por login correcto. Usar navegador/cliente API local sin guardar capturas de tokens. No usar una cuenta real ni el único superadmin.

1. Registrar defaults o valores efectivos AUTH_LOGIN_*. Cerrar sesión. No reiniciar backend durante la prueba.
2. Desde dispositivo A hacer 5 logins incorrectos con el mismo DNI, separados al menos 7 segundos (para no chocar con el rate limit Nginx). Esperar 7 segundos y enviar la contraseña correcta: esperar 401 mientras dure el bloqueo backend.
3. Repetir contraseña correcta variando headers X-Forwarded-For, X-Real-IP y Forwarded cada vez: debe seguir 401. Si devuelve 200, detener piloto y revisar proxy. Correlacionar IP normalizada en frontend.
4. Desde dispositivo B con **otra IP pública** el login correcto debe funcionar: confirma que la clave incorpora IP, no solo DNI. Logout inmediatamente. Si A y B comparten NAT, no sirve para esta aserción.
5. Esperar `AUTH_LOGIN_BLOCK_MINUTES` completos desde el último fallo que disparó bloqueo, sin ataques intermedios. En A, contraseña correcta → 200, `/api/auth/me` → 200, logout → 200 y mismo token → 401. Un 429 indica Nginx; esperar que drene antes de evaluar backend.
6. Probar ráfaga breve de peticiones incorrectas desde A, incluso alternando DNI ficticio: Nginx debe devolver 429. No medir esto con datos reales ni durante otras pruebas manuales.

Resultado esperado documenta la IP realmente usada por el contador mediante aislamiento entre A/B y resistencia a spoof; no se agrega un endpoint de diagnóstico ni se imprimen tokens.

El grupo Docker y usuarios del host son administradores efectivos: esta frontera no protege contra alguien con control del daemon/host. Backend/MySQL no publican puertos; no conectar contenedores ajenos a `private`.
