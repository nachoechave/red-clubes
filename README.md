# Red Clubes

Sistema web multi-club para gestionar socios, actividades, inscripciones, cuotas, pagos, asistencia, usuarios y reportes. Está pensado para clubes barriales, centros de jubilados y un eventual uso municipal.

El despliegue previsto tiene dos etapas: piloto en un Cloud Server portable —por ejemplo Donweb— y, si la Municipalidad adopta la propuesta, migración al dominio e infraestructura asociados a un subdominio de `ensenada.gov.ar`.

## Estado

El núcleo del MVP interno está implementado y compila. La última verificación local ejecutó 37 pruebas backend, 13 frontend, el empaquetado Spring Boot y el build Angular de producción. También construyó las imágenes Linux, levantó MySQL 8.4, backend y frontend con Compose, aplicó seis migraciones Flyway, aprobó health checks y verificó backup/restauración con recuperación de datos.

No se debe usar todavía con datos personales reales hasta probar Compose, backups, restauración, TLS y smoke tests en el Cloud Server elegido.

## Funcionalidades

- Login, logout real, sesión restaurable y cambio obligatorio de contraseña inicial.
- Roles globales y por club para superusuario, administrador, operador y profesor; asignación de actividades a profesores.
- Alta, edición, búsqueda y baja lógica de socios.
- Alta, edición, activación y desactivación de actividades con profesor responsable y cupo.
- Inscripciones con historial, reglas de club/estado/cupo y contador derivado.
- Asistencia presente, ausente o justificada solo para socios inscriptos.
- Cuotas manuales y generación mensual idempotente.
- Pagos totales, medios de pago, historial y anulación trazable.
- Dashboard mensual y reportes anuales basados en pagos/asistencias reales.
- Administración de usuarios y permisos con aislamiento multi-club.
- Auditoría consultable por club de cambios sensibles.

Fuera de este MVP: pagos parciales, pasarela de cobro, WhatsApp, QR, aplicación móvil y módulo de eventos completo.

## Roles

- `SUPERUSUARIO`: alcance global.
- `ADMINISTRADOR`: administra solamente clubes asignados, incluida la gestión de usuarios locales y auditoría.
- `OPERADOR`: realiza la gestión diaria de socios, inscripciones, cuotas, reportes y asistencia en sus clubes, sin administrar usuarios, actividades ni auditoría.
- `PROFESOR`: ve actividades asignadas y toma asistencia bajo sus reglas.

La matriz completa está en [roles y permisos](docs/ROLES_Y_PERMISOS.md).

## Arquitectura y versiones

- Angular 21.2, TypeScript 5.9 y Node 22.23.2.
- Java 21 y Spring Boot 3.5.0.
- MySQL 8.4 en Compose; H2 2.3 en pruebas.
- Flyway, Spring Security, Maven Wrapper, Nginx y Docker Compose.

Ver [arquitectura](docs/ARQUITECTURA.md) y [modelo de datos](docs/MODELO_DE_DATOS.md).

## Requisitos

Para ejecución manual: JDK 21, Node 22.23.2, npm y MySQL 8. Para ejecución completa: Docker Engine con Compose v2. En Windows puede usarse Docker Desktop con contenedores Linux.

## Variables de entorno

Copiá `.env.example` a `.env` y reemplazá todos los `change_me`. Las variables centrales son:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
- `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD` para Compose.
- `CORS_ALLOWED_ORIGINS` o `PUBLIC_ORIGIN`.
- `AUTH_SESSION_HOURS` y parámetros `AUTH_LOGIN_*`.
- `FLYWAY_BASELINE_ON_MIGRATE=false` salvo adopción controlada.
- `DEMO_DATA_ENABLED=false` y `BOOTSTRAP_SUPERUSER_ENABLED=false` en producción.

`.env` está ignorado por Git. No uses `root` como usuario normal de la aplicación.

## Ejecución con Docker

```bash
cp .env.example .env
# editar secretos y origen público
docker compose config --quiet
docker compose up --build -d
docker compose ps
```

Por defecto la app queda en `http://localhost:8080`. MySQL y backend no publican puertos. Para detener sin borrar datos: `docker compose down`. No agregues `-v` salvo que quieras eliminar el volumen de MySQL de forma consciente.

## Ejecución manual

Backend, con variables de `.env.example` exportadas:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd frontend
nvm use
npm ci
npm start
```

Angular abre `http://localhost:4200` y redirige `/api` a `localhost:8080` mediante `proxy.conf.json`.

## Migraciones

Producción usa `ddl-auto=validate`; el esquema cambia únicamente mediante `backend/src/main/resources/db/migration`. Una base nueva aplica V1–V6. Una base existente requiere dump, inventario de duplicados, ensayo sobre una copia y el procedimiento de [migraciones](docs/GUIA_MIGRACIONES.md).

V4 revoca sesiones existentes una sola vez para adoptar hashes de bearer. Es esperado que los usuarios deban volver a iniciar sesión después de esa migración.

## Pruebas y build

```powershell
cd backend
.\mvnw.cmd clean verify
```

```powershell
cd frontend
nvm use
npm ci
npm test -- --watch=false
npm run build -- --configuration production
```

GitHub Actions repite backend, frontend y construcción de contenedores en cada push y pull request.

## Backups

En un host Linux con el stack levantado:

```bash
sh ops/backup-mysql.sh
CONFIRM_RESTORE=RESTAURAR sh ops/restore-mysql.sh backups/red-clubes-<fecha>.sql.gz
```

El backup se comprime, se verifica y aplica retención local de 14 días por defecto. Definí `BACKUP_DIR` y `BACKUP_RETENTION_DAYS` si corresponde. Copiá luego el archivo cifrado a una ubicación externa; un backup dentro del mismo servidor no cubre pérdida del host. Toda restauración debe probarse fuera de producción.

## Estructura

```text
backend/                 API, dominio, seguridad, migraciones y pruebas
frontend/                Angular, core/auth, rutas, interfaz y Nginx
docs/                    auditoría, roadmap y guías técnicas/operativas
ops/                     backup y restauración
.github/workflows/ci.yml integración continua
docker-compose.yml       MySQL + backend + frontend
```

## Endpoints principales

- `POST /api/auth/login`, `POST /api/auth/logout`, `GET/PUT /api/auth/me`.
- `/api/clubes` y `/api/clubes/{clubId}/socios`.
- `/api/clubes/{clubId}/actividades` e inscripciones por socio.
- `/api/clubes/{clubId}/actividades/{id}/asistencias/{fecha}`.
- `/api/clubes/{clubId}/cuotas`, generación y pagos.
- `/api/clubes/{clubId}/dashboard`, `/reportes` y `/auditoria`.
- `/api/usuarios` y `GET /api/health`.

Todos salvo health y login requieren bearer. La autorización concreta depende de rol y club.

## Seguridad

Los bearers son opacos y en base solo se guarda SHA-256; las contraseñas tienen hash con salt. Spring Security protege `/api/**` por defecto, CORS es configurable, Nginx agrega CSP y headers defensivos, y aplicación/proxy limitan intentos de login. Los errores no exponen stack traces. Ver [seguridad](docs/SEGURIDAD.md).

## Despliegue

La [guía de despliegue](docs/GUIA_DESPLIEGUE.md) cubre el Cloud Server piloto, TLS, DNS, backups, rollback y la migración posterior a `ensenada.gov.ar`. El proveedor no está codificado en la aplicación.

## Capturas

Antes de presentar el piloto se deben agregar capturas sin datos personales de login, dashboard, socios, actividad/asistencia y cuotas/reportes bajo `docs/capturas/`.

## Documentación y roadmap

- [Auditoría técnica](docs/AUDITORIA_TECNICA.md)
- [Roadmap de profesionalización](docs/ROADMAP_PROFESIONALIZACION.md)
- [Arquitectura](docs/ARQUITECTURA.md)
- [Modelo de datos](docs/MODELO_DE_DATOS.md)
- [Roles y permisos](docs/ROLES_Y_PERMISOS.md)
- [Seguridad](docs/SEGURIDAD.md)
- [Guía de migraciones](docs/GUIA_MIGRACIONES.md)
- [Guía de despliegue](docs/GUIA_DESPLIEGUE.md)

## Limitaciones conocidas

- Falta ensayar V1–V6 y restauración contra una copia MySQL real preexistente.
- El frontend conserva parte de la orquestación de features en el componente raíz; core auth y routing ya están separados.
- El límite de login es local a una instancia.
- No existe aún monitoreo externo ni dominio/TLS del piloto contratados.
- El workflow CI está corregido y su equivalente local aprobó; falta confirmar una ejecución remota verde después de publicar estos cambios.

No se deben presentar esas limitaciones como funcionalidades terminadas; son los últimos criterios operativos para habilitar un piloto real.
