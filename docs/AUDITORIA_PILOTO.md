# Auditoría previa al piloto

Corte: 2026-09-16. Base exclusiva: `origin/main` en `994134d04582ba581ec6fe73fa6f516c24b336c8`, merge del PR #3. Árbol limpio, idéntico al último commit de hardening; rama nueva `chore/pilot-deployment`. GitHub confirma backend, frontend, secrets, backend-dependencies y containers en success. No se vuelven a implementar esos controles.

## Inspección anterior a los cambios

| Componente | Evidencia y decisión |
|---|---|
| Compose | MySQL/backend solo `private` interna; frontend además `edge`, bind loopback, restart `unless-stopped`, health y límites. Conservar. Gateway de `edge` dinámico: no asumir que el peer del proxy es `172.30.0.1`. |
| Dockerfiles | Java 21/Boot 3.5, Node 22/Angular 21, actualización Alpine, usuarios no root. Conservar versiones. |
| Nginx | Sobrescribe headers al backend. `location /index.html` define `add_header Cache-Control` y corta la herencia de headers defensivos; corregir la ubicación del header y comprobar en smoke. El HTML Angular optimizado usa `onload` inline incompatible con la CSP existente: desactivar solo inlineCritical en build. |
| Entorno | Nombres reales `BOOTSTRAP_SUPERUSER_*`, no `SUPERUSER_BOOTSTRAP_*`. Compose deriva DB_URL/DB_USERNAME/DB_PASSWORD y fuerza prod/Flyway. Crear ejemplo exclusivo Compose para evitar contradicciones. |
| Flyway | V1–V6, prod `ddl-auto=validate`. Sin modificaciones de migraciones. Histórico se trata por separado. |
| Backup | Dump/checksum/retención funcionan. `.last-success` se escribía antes de exportar: no demostraba copia externa exitosa. Hook podía copiar plaintext y no verificaba ciphertext ni retenía `.age`. Ajustar para operación diaria cifrada verificable. |
| Restore | Doble confirmación y backup previo existentes. Checksum era opcional; hacerlo obligatorio antes de detener servicios. Restore aislado presupone historial Flyway, por eso no sirve sin adaptación para una base pre-Flyway. |
| Smoke | 200/401/404 existentes, auth opcional. Agregar timeout, headers, JSON escapado, credenciales por archivo temporal privado y logout/revocación. |
| Logs | Único logger de aplicación encontrado: fallo de health genérico. Prod oculta SQL/stacktrace HTTP; logs de librerías/errores de DB aún pueden incluir datos. Nginx por defecto registra request con query: usar formato sin query/body/Authorization. No habilitar DEBUG/TRACE para diagnosticar IP. |
| Demo | No crea usuarios. Usa números y contactos de apariencia real; mantener `DEMO_DATA_ENABLED=false`. Usar nombres PILOTO y correos .invalid; identificadores sintéticos de diez dígitos 9900000000+ para respetar validación numérica de login/socios y VARCHAR(10), sin DNIs de personas. |
| CI | Mantener todos los scanners, umbrales y tests. Agregar validaciones de scripts y configuraciones nuevas. |
| Documentos | README aún describía la rama previa como pendiente. Guías confundían gateway privado con proxy real; faltaban emisión TLS inicial, timers y prueba de reboot. Se conservan documentos históricos y se enlaza runbook específico. |

## Límites

Sin cambios de negocio, entidades, UI, dependencias mayores ni arquitectura distribuida. Ningún cambio/merge de Dependabot. Ningún servidor real disponible en esta tarea. DNS/TLS/firewall/IP real, reboot, copia externa, aceptación multi-club y recuperación real requieren evidencia en destino; CI no los acredita.

Fuente de la regla de herencia: [Nginx add_header](https://nginx.org/en/docs/http/ngx_http_headers_module.html#add_header). Resultado final de verificaciones: [informe](INFORME_PILOTO.md).
