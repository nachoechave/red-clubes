# Roadmap de profesionalización

Estados: `PENDIENTE`, `EN CURSO`, `BLOQUEADO`, `HECHO`.
Prioridades: `P0` crítica, `P1` alta, `P2` media, `P3` mejora.

## Fase 1 — Baseline y hardening inmediato

| ID | Tarea | Prioridad | Dependencias | Riesgo | Criterio de aceptación | Estado | Archivos estimados |
|---|---|---:|---|---|---|---|---|
| SEC-01 | Externalizar credenciales y perfiles | P0 | Ninguna | Bajo | No hay secretos reales; app usa variables; `.env` ignorado; ejemplo seguro presente | HECHO | `application*.properties`, `.env.example`, `.gitignore` |
| SEC-02 | Eliminar bootstrap privilegiado predecible | P0 | SEC-01 | Bajo | No se crea cuenta por defecto; aprovisionamiento es opt-in y exige variables | HECHO | `SuperUsuarioInitializer.java` |
| SEC-03 | Retirar endpoints legacy públicos de socios | P0 | Ninguna | Medio | No existe acceso a socios fuera de rutas autenticadas por club; frontend vigente no se rompe | HECHO | `SocioController.java` |
| SEC-04 | Corregir IDOR de socios y cuotas | P0 | SEC-03 | Medio | Todo update/baja/pago carga por `id + clubId`; tests prueban Club A contra Club B | HECHO | repositories, services, controllers y tests |
| SEC-05 | Restringir administración de usuarios | P0 | Matriz actual de roles | Medio | Admin local solo ve/opera usuarios completamente dentro de sus clubes; superusuario conserva alcance global | HECHO | `UsuarioController.java`, `UsuarioService.java`, tests |
| SEC-06 | Corregir contrato 401/403 y logout | P0 | Sesiones actuales | Bajo | Sin/invalid token devuelve 401; permiso insuficiente 403; logout revoca token | HECHO | auth, exception handler y tests |
| TEST-01 | Perfil de test aislado | P0 | H2 test | Bajo | `mvn clean verify` no depende de MySQL instalado | HECHO | `pom.xml`, `src/test/resources`, tests |
| TEST-02 | Reparar baseline frontend | P1 | Ninguna | Bajo | Tests generados representan la UI y `npm test -- --watch=false` pasa | HECHO | `app.spec.ts` |
| DOC-01 | Auditoría y roadmap | P0 | Inventario/builds | Bajo | Documentos reflejan evidencia, brechas y orden verificable | HECHO | `docs/AUDITORIA_TECNICA.md`, este archivo |

## Fase 2 — Seguridad central e integridad de base

| ID | Tarea | Prioridad | Dependencias | Riesgo | Criterio de aceptación | Estado | Archivos estimados |
|---|---|---:|---|---|---|---|---|
| SEC-07 | Centralizar autenticación/autorización | P0 | SEC-06, decisión técnica | Alto | Spring Security o interceptor cubre `/api/**` por defecto; solo health/login públicos; pruebas 401/403 | HECHO | `pom.xml`, `config/security/**`, controladores, tests |
| SEC-08 | Hash de tokens y limpieza de sesiones | P1 | SEC-07 | Medio | Base no guarda bearer utilizable; expiradas se purgan; token único e indexado | HECHO | sesión, auth, migración, tests |
| SEC-09 | Protección de login | P1 | SEC-07 | Medio | Límite configurable por identidad/IP, sin enumeración de usuarios y con tests | HECHO | auth, config, tests |
| DB-01 | Inventario y backup de esquema real | P0 | Acceso a copia MySQL | Alto | Dump verificado, conteos registrados y plan de rollback documentado | PENDIENTE | `docs/`, scripts seguros |
| DB-02 | Baseline Flyway no destructivo | P0 | DB-01 | Alto | Base existente adopta baseline; base nueva se crea desde cero; prod usa `validate` | EN CURSO | `pom.xml`, `db/migration/**`, profiles |
| DB-03 | Constraints e índices multi-club | P0 | DB-02, saneamiento | Alto | Duplicados detectados/saneados; constraints e índices documentados y probados | EN CURSO | migraciones, repositories, tests |
| API-01 | DTOs y errores uniformes | P1 | DB-03 | Medio | Ningún controlador expone JPA; validación produce contrato estable con timestamp/código/campos | HECHO | DTOs, controllers, handler, tests |
| AUD-01 | Auditoría sensible | P1 | SEC-07, DB-02 | Medio | Registra actor, club, acción, entidad, ID y fecha sin secretos | HECHO | entidad/migración/servicio/tests |

## Fase 3 — Inscripciones, actividades y asistencia

| ID | Tarea | Prioridad | Dependencias | Riesgo | Criterio de aceptación | Estado | Archivos estimados |
|---|---|---:|---|---|---|---|---|
| INS-01 | Consolidar modelo de inscripción | P0 | DB-03, API-01 | Medio | Estado, fechas, observaciones y actor; socio/actividad/club coherentes | HECHO | gestión/inscripciones, migración, tests |
| INS-02 | Reglas de cupo, estado y duplicado | P0 | INS-01 | Medio | No duplica activa, no supera cupo ni acepta socio/actividad inactivos; tests obligatorios | HECHO | `GestionService`, repository, tests |
| ACT-01 | Profesor como relación | P1 | Matriz de roles, DB-02 | Alto | Actividad referencia usuario profesor válido; migración preserva texto histórico | HECHO | actividad/usuario, migración, UI |
| ACT-02 | Cupos derivados | P1 | INS-01 | Medio | Inscriptos se calcula desde activas; se elimina contador mutable con migración segura | EN CURSO | actividad, queries, DTO, UI |
| ASI-01 | Estados y trazabilidad de asistencia | P1 | INS-01, AUD-01 | Medio | Solo inscriptos; único por fecha; estados claros; actor y fecha real | HECHO | asistencia, migración, API, UI, tests |

## Fase 4 — Cuotas y pagos

| ID | Tarea | Prioridad | Dependencias | Riesgo | Criterio de aceptación | Estado | Archivos estimados |
|---|---|---:|---|---|---|---|---|
| COB-01 | Rediseñar período e importe de cuota | P0 | DB-02 | Alto | Período inequívoco, `BigDecimal`, fechas/estados; migración preserva datos | HECHO | cuota, migración, DTOs, tests |
| COB-02 | Entidad Pago y anulación | P0 | COB-01, AUD-01 | Alto | Historial inmutable, anulación auditada, sin pagos parciales en MVP | HECHO | pago, migración, service/controller/UI/tests |
| COB-03 | Generación mensual idempotente | P1 | COB-01 | Medio | Repetir generación no duplica socio+período; transacción y constraint | HECHO | servicio, API, scheduler, tests |
| COB-04 | Deuda, recaudación e historial | P1 | COB-02 | Medio | Filtros por club/período/estado y totales monetarios correctos | HECHO | queries, DTOs, UI, tests |

## Fase 5 — Dashboard, frontend e infraestructura

| ID | Tarea | Prioridad | Dependencias | Riesgo | Criterio de aceptación | Estado | Archivos estimados |
|---|---|---:|---|---|---|---|---|
| REP-01 | Definir métricas y filtros | P1 | INS, COB, ASI | Medio | Mes/año/rango/actividad/estado; definiciones documentadas; tests de fechas | HECHO | dashboard/reportes, queries, UI |
| FE-01 | Extraer `core/auth` | P0 | SEC-07 | Medio | servicio, interceptor, guard, restauración y errores centralizados con tests | HECHO | `frontend/src/app/core/**` |
| FE-02 | API URL por entorno/runtime | P0 | FE-01 | Bajo | Producción consume `/api` en el mismo subdominio; cero URLs absolutas hardcodeadas y configuración dev/prod comprobada | HECHO | config, interceptor, proxy, tests |
| FE-03 | Rutas y features lazy | P1 | FE-01 | Alto | Rutas solicitadas, 404 y role guard; extracción incremental sin reescritura total | HECHO | routes, `features/**`, template/styles |
| FE-04 | Formularios y accesibilidad | P1 | FE-03 | Medio | Reactive Forms críticos, doble envío, mensajes, teclado y contraste verificados | EN CURSO | features/shared y tests |
| FE-05 | Runtime de build reproducible | P0 | TEST-02 | Bajo | Node fijado, versión inválida falla temprano y build de producción pasa | HECHO | `.nvmrc`, `package.json`, `scripts/verify-node.cjs` |
| INF-01 | Docker reproducible | P1 | DB-02, FE-02 | Medio | MySQL/backend/frontend levantan con Compose y health checks | HECHO | Dockerfiles, compose, env example |
| INF-02 | Piloto en Cloud Server | P1 | INF-01, FE-02, contratación del host | Medio | Docker Compose, DNS piloto, TLS, proxy `/api`, backup, límites y rollback verificados | PENDIENTE | compose, proxy, certificados, runbook, smoke tests |
| INF-03 | Migración a subdominio municipal | P1 | INF-02, aprobación e infraestructura municipal | Medio | Datos exportados/importados, DNS, TLS, secretos y operación municipal verificados sin dependencia de Donweb | PENDIENTE | runbook de migración, configuración, smoke tests |
| CI-01 | GitHub Actions | P1 | TEST-01, TEST-02 | Bajo | Backend verify + frontend install/test/build + stack Docker y health checks; falta confirmar ejecución remota después del próximo push | EN CURSO | `.github/workflows/ci.yml` |
| DOC-02 | Documentación operativa completa | P1 | Fases previas | Bajo | README y cinco documentos exigidos describen solo lo implementado | HECHO | `README.md`, `docs/**` |

## Decisiones que requieren revisión antes de avanzar

1. **Seguridad central:** Spring Security mantiene sesiones opacas; bearer de 256 bits, hash persistido, limpieza periódica y límite de login ya están implementados.
2. **Baseline Flyway:** ya existe un baseline reproducible para bases nuevas. La adopción de una base existente continúa condicionada a inventario, backup, saneamiento de duplicados y ensayo sobre una copia; no se deben aplicar constraints directamente sobre producción.
3. **Profesor de actividad:** se aplicó relación nullable a `Usuario` y se conserva el texto histórico. Las coincidencias antiguas no se vinculan automáticamente para evitar asociaciones ambiguas.
4. **Modelo cuota/pago:** `Pago` y la importación histórica están implementados; falta validarlos sobre una copia MySQL real antes de adoptar datos productivos.
