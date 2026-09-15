# Observabilidad mínima

El piloto no necesita una plataforma distribuida, pero sí señales simples, accionables y con responsable.

## Señales

- Disponibilidad externa: `GET https://<dominio>/healthz` cada minuto.
- Aplicación + base: `GET https://<dominio>/api/health`; devuelve `200 {"status":"UP"}` solo si `SELECT 1` funciona.
- Contenedores: `docker compose ps`, estados `healthy`, reinicios y OOM.
- Host: CPU, RAM/swap, disco libre, inodos y carga.
- MySQL: tamaño del volumen, conexiones, latencia y fallos de backup.
- Seguridad: tasas de `401`, `403`, `429` y `5xx`, sin registrar credenciales.
- Recuperación: antigüedad de `.last-success`, tamaño/checksum y fecha del último restore aislado.

## Opción de bajo costo

Usar Uptime Kuma o el monitor HTTP del proveedor para `/healthz` y `/api/health`, más Netdata o las métricas básicas del Cloud Server para host/contenedores. Enviar alertas a un canal con guardia real (correo y un segundo medio). Esto no requiere modificar la aplicación.

## Umbrales iniciales

- Dos fallos consecutivos de health: alerta alta.
- Disco >80 % o backup con más de 26 horas: alerta alta.
- Memoria >85 % durante 15 minutos, swap sostenido o reinicio/OOM: alerta alta.
- `5xx` sostenidos o aumento anormal de `429`: investigación.
- Certificado con menos de 21 días: alerta; menos de 7 días: crítica.

Los umbrales se ajustan con medición real. Una alerta sin destinatario, ventana de atención y procedimiento no es un control.

## Consulta operativa

```bash
docker compose ps
docker compose logs --since=30m --tail=300 backend frontend mysql
docker stats --no-stream
df -h
```

Los logs rotan localmente. Si se centralizan, usar transporte cifrado, acceso mínimo y retención acordada; no enviar dumps ni secretos.

## Health, backup y mantenimiento

- El health público revela solo `UP`/`DOWN`.
- Programar backup diario y alertar si el comando devuelve error.
- Ejecutar restore aislado mensual y registrar archivo, checksum, duración y conteos.
- Revisar actualizaciones Dependabot y alertas de CI semanalmente.
- Ensayar semestralmente caída, rollback y restauración.
