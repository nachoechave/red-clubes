# Auditoría técnica de Red Clubes

Fecha de corte: 15 de septiembre de 2026.

Base inspeccionada: `main` en `5684b60`.

Rama de trabajo: `chore/production-readiness`.

Alcance: seguridad, configuración productiva, Docker, proxy, CI, backups, health, observabilidad, migraciones y documentación. No se agregaron funcionalidades de negocio.

## Estado inicial encontrado

El MVP ya tenía mejoras importantes: Spring Security con denegación por defecto, sesiones opacas hasheadas, límite de login en memoria, Flyway V1–V6, `ddl-auto=validate`, aislamiento multi-club probado, Dockerfiles, Compose, health checks básicos, scripts de backup/restore y CI de build/test/stack.

Los principales riesgos operativos aún abiertos eran:

| Riesgo inicial | Impacto |
|---|---|
| PBKDF2 en 120.000 iteraciones | Menor costo para ataque offline si se filtra la base. |
| Frontend publicado en todas las interfaces | Posible bypass del reverse proxy/TLS/firewall esperado. |
| Nginx sin frontera explícita de proxy confiable | Rate limit basado en proxy o headers falsificables según topología. |
| Imágenes/runtime parcialmente endurecidos | Frontend root, escritura amplia y sin límites de procesos/recursos. |
| Sin rotación de logs ni presupuesto 2 vCPU/4 GB | Riesgo de agotar disco/RAM y degradar todo el host. |
| Backup directo al archivo final y restore sin ensayo automatizado | Archivo parcial podía parecer válido; recuperación no demostrable. |
| CI sin secret/SCA/image scanning | Regresiones de secretos o CVE podían entrar con tests verdes. |
| Health backend solo de proceso | Podía reportar OK con base inaccesible. |
| Documentación desactualizada | Despliegues inseguros o afirmaciones incompatibles con el código real. |

No se encontró `.env` versionado en el estado actual; `.env` y `backups/` estaban ignorados y `.env.example` contenía placeholders. La revisión por nombres/patrones no encontró claves privadas ni tokens de formatos comunes. Gitleaks queda como control de historial completo en CI, porque la inspección manual no demuestra ausencia histórica.

## Cambios realizados

### Contraseñas y login

- PBKDF2-HMAC-SHA256 elevado a 600.000 iteraciones para hashes nuevos.
- El número de iteraciones permanece dentro del formato persistido: los hashes de 120.000 siguen validándose.
- Rehash progresivo transaccional después de un login correcto.
- Parser defensivo, límite superior de costo y comparación `MessageDigest.isEqual`.
- Pruebas unitarias e integración de compatibilidad, rechazo y actualización.

### Red, proxy y contenedores

- Puerto frontend ligado a `127.0.0.1`; backend/MySQL sin puertos host; red Docker interna.
- Subred/gateway explícitos y `TRUSTED_PROXY_CIDR` exacto para recuperar IP real.
- Nginx normaliza headers, limita login por IP, devuelve `429`, acota conexiones/cuerpo y agrega headers defensivos.
- Backend y Nginx corren sin root, sin nuevos privilegios y con root filesystem de solo lectura.
- CPU, memoria, PIDs, heap Java, shutdown, pool Hikari, request size, timezone y logs rotados/configurados.
- Health backend comprueba `SELECT 1` y devuelve solo `UP`/`DOWN`.

### Datos y continuidad

- MySQL 8.4 con `utf8mb4`, zona `-03:00`, buffer/conexiones acotados y usuario de aplicación no root.
- Backup atómico, `umask 077`, dump con usuario app, validación de contenido/gzip, SHA-256, marcador de éxito y retención.
- Hook desacoplado para cifrado `age` y/o copia externa.
- Restore productivo con checksum, doble confirmación, detención de backend y backup previo.
- Restore de ensayo en MySQL descartable sin puerto/red externa; verifica tablas e historial Flyway.
- Smoke test parametrizable con health, 200/401/404, login opcional y HTTPS.

### Cadena de suministro y operación

- CI suma Gitleaks v8.30.1, npm audit high y Trivy v0.36.0 sobre el JAR Maven y ambas imágenes.
- Dependabot semanal para Actions, Maven, npm y Docker.
- Imágenes/tag coordinados mediante `IMAGE_TAG`; documentación SemVer, digest, actualización y rollback.
- Runbook de 22 pasos, seguridad, migraciones, observabilidad, backups y checklist reconciliados.

## Auditoría de Flyway e integridad

Las V1–V6 ya contienen constraints e índices para las consultas críticas: unicidad por club, inscripciones/asistencias, cuotas/período, sesiones, pagos, auditoría y profesor. No se detectó una modificación de esquema necesaria para este hardening, por lo que **no se creó V7 ni se editó una migración publicada**.

Una instalación limpia se valida con tests H2 en modo MySQL, pero la adopción de una base histórica continúa requiriendo inventario, saneamiento y ensayo en MySQL 8.4. `FLYWAY_BASELINE_ON_MIGRATE` permanece `false` por defecto.

## Riesgos corregidos

| Riesgo | Solución aplicada |
|---|---|
| Ataque offline más barato | PBKDF2 600k + rehash gradual compatible. |
| Bypass del proxy | Bind loopback + red interna + ningún puerto de DB/backend. |
| IP falsa/rate limit incorrecto | Trust CIDR exacto, real IP recursive y headers normalizados. |
| Escalamiento/abuso de runtime | Usuarios no root, read-only, no-new-privileges, PIDs/CPU/RAM. |
| Disco agotado por logs | Rotación `json-file` por tamaño/cantidad. |
| Backup parcial/no comprobable | Publicación atómica, validación y checksum. |
| Restore sobre producción durante pruebas | Script aislado y procedimiento productivo con guardas. |
| Vulnerabilidades no bloqueadas | Gitleaks, npm audit y Trivy para Maven/imágenes en CI. |
| Health falso positivo sin base | `SELECT 1` dentro de `/api/health`. |

## Riesgos que continúan

### BLOQUEANTE para datos reales

- El Cloud Server, DNS, TLS, firewall, proxy real, alertas y backups externos todavía deben configurarse/verificarse en destino.
- Si existe una base preexistente, sus V1–V6, constraints y datos deben ensayarse sobre copia MySQL antes de adopción.
- Cualquier credencial históricamente expuesta debe rotarse; Gitleaks no revoca secretos.

### IMPORTANTE

- Rate limit backend en memoria: una instancia y pérdida de contador tras reinicio.
- Sin alta disponibilidad; la restauración define el RTO y debe medirse mensualmente.
- CSP aún permite estilos inline por compatibilidad Angular.
- Los feeds externos de Trivy pueden requerir caché/mirror si el proveedor limita descargas; una supresión exige evidencia y vencimiento.

### MEJORA

- Reducir el componente Angular raíz y ampliar pruebas E2E/accesibilidad.
- Centralizar logs/métricas si el piloto crece.
- Separar credenciales de migración y runtime si MySQL requiere privilegio mínimo más estricto.

## Verificación

Los resultados exactos del último cierre se mantienen en el reporte de la tarea/PR. El estándar obligatorio es:

- `backend/.\mvnw.cmd clean verify`
- `frontend/npm ci`
- `frontend/npm test -- --watch=false`
- `frontend/npm run build`
- `docker compose config --quiet`
- `docker compose build`
- `docker compose up -d --wait`, health y `ops/smoke-test.sh`
- `ops/backup-mysql.sh` y `ops/restore-test.sh` sobre entorno no productivo
- Gitleaks, npm audit y Trivy (JAR + imágenes) en CI

Toda prueba no ejecutada debe declararse `NO VERIFICADO`; no se infiere por documentación ni por una ejecución anterior.

## Dictamen

**Candidato técnico para piloto controlado, condicionado a validación de infraestructura destino y cierre de todos los controles bloqueantes.** No se debe afirmar que está listo para datos reales hasta completar la checklist de despliegue.
