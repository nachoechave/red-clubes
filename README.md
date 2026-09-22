# Red Clubes

Aplicación web multi-club para administrar socios, actividades, inscripciones, asistencia, cuotas, pagos, usuarios, auditoría y reportes. El repositorio contiene un monolito Spring Boot + Angular, MySQL y una topología Docker Compose portable para un piloto de bajo volumen.

## Estado del proyecto

El código está preparado como **candidato a piloto controlado**. No equivale a una autorización para cargar datos personales reales: antes se debe completar un despliegue en infraestructura destino, configurar DNS/TLS, restaurar un backup de ensayo y cerrar la checklist operativa de [la guía de despliegue](docs/GUIA_DESPLIEGUE.md).

La preparación productiva del PR #3 ya fue integrada en `main`. Los cambios nuevos deben ingresar mediante Pull Request y con CI verde.

## Trabajo académico 2026

Para el trabajo grupal de facultad se conserva un punto de partida explícito:

- `facultad`: snapshot del código previo al trabajo del equipo;
- `develop`: rama de integración del trabajo académico;
- `feature/*`, `fix/*` y `docs/*`: ramas por tarea;
- `main`: versión estable general.

La trazabilidad académica se documenta en:

- [Baseline académico](docs/BASELINE.md)
- [Alcance del TP](docs/ALCANCE_TP.md)
- [Contribuciones](docs/CONTRIBUCIONES.md)

Las ramas de trabajo deben salir de `develop` y volver mediante Pull Request. No se reescribe el historial previo para atribuir código anterior a integrantes que se incorporan al proyecto.

## Stack soportado

- Java 21, Spring Boot 3.5, Maven Wrapper y Flyway.
- Angular 21.2, TypeScript 5.9, Node 22.23.2 y npm.
- MySQL 8.4, Nginx no privilegiado y Docker Compose v2.
- H2 solo para pruebas automatizadas.

## Inicio rápido local con Docker

```bash
cp .env.example .env
# Reemplazar todos los change_me y revisar el origen público.
docker compose config --quiet
docker compose build
docker compose up -d --wait
BASE_URL=http://127.0.0.1:8080 sh ops/smoke-test.sh
```

La única publicación de Compose es `127.0.0.1:${APP_PORT:-8080}`. MySQL y backend permanecen en una red interna. En producción, un reverse proxy instalado en el host entrega HTTPS y reenvía a ese puerto local.

Para detener sin borrar datos:

```bash
docker compose down
```

No agregues `-v`: elimina el volumen de MySQL.

## Ejecución manual

Backend, con las variables de `.env.example` exportadas:

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

Angular sirve en `http://localhost:4200` y deriva `/api` al backend mediante `proxy.conf.json`.

## Controles de seguridad relevantes

- Spring Security deniega por defecto toda API salvo login, health y preflight.
- Sesiones opacas de 256 bits; la base conserva solo SHA-256 del bearer.
- Contraseñas PBKDF2-HMAC-SHA256 con salt y 600.000 iteraciones; hashes anteriores se validan y actualizan al iniciar sesión.
- Límite de login en Nginx por IP y en backend por identidad + IP.
- Nginx confía headers de cliente solo desde el gateway Docker configurado.
- Contenedores de backend/frontend sin root, root filesystem de solo lectura y sin privilegios nuevos.
- `.env` y `backups/` ignorados; CI ejecuta Gitleaks, npm audit y Trivy sobre dependencias Maven/imágenes.
- CSP y headers defensivos; el reverse proxy público debe manejar TLS y redirección HTTP→HTTPS.

Ver [seguridad](docs/SEGURIDAD.md).

## Pruebas

```powershell
cd backend
.\mvnw.cmd clean verify
```

```powershell
cd frontend
npm ci
npm test -- --watch=false
npm run build
```

La validación integral agrega `docker compose config`, construcción de imágenes, health checks, [smoke test](ops/smoke-test.sh), [backup](ops/backup-mysql.sh) y [restore aislado](ops/restore-test.sh). GitHub Actions repite estas pruebas y bloquea hallazgos de seguridad altos/críticos.

## Datos y migraciones

Producción usa `spring.jpa.hibernate.ddl-auto=validate`. Flyway es la única autoridad del esquema y las migraciones aplicadas nunca se editan. Una base preexistente se adopta únicamente sobre una copia, después de inventario, saneamiento y backup verificable. Ver [guía de migraciones](docs/GUIA_MIGRACIONES.md).

El usuario de aplicación es `MYSQL_USER`; no es root. `MYSQL_ROOT_PASSWORD` queda reservado al bootstrap/operación del contenedor y no se usa desde la aplicación ni desde el backup normal.

## Backup y recuperación

```bash
sh ops/backup-mysql.sh
sh ops/restore-test.sh backups/red-clubes-<fecha>.sql.gz
```

El backup es atómico, privado por `umask`, comprimido, validado, acompañado por SHA-256 y sujeto a retención. `BACKUP_EXPORT_HOOK` permite cifrar con `age` y/o copiar fuera del host usando [el hook de ejemplo](ops/backup-export-hook.example.sh).

La restauración productiva es deliberadamente explícita, detiene backend y crea un backup previo:

```bash
CONFIRM_RESTORE=RESTAURAR ALLOW_PRODUCTION_RESTORE=true \
  sh ops/restore-mysql.sh backups/red-clubes-<fecha>.sql.gz
```

## Versionado y releases

Usar SemVer (`vMAJOR.MINOR.PATCH`) y el mismo tag inmutable para Git, `IMAGE_TAG`, imagen backend e imagen frontend. No desplegar `latest`. Cada release debe registrar migraciones incluidas, resultado de CI, backup previo y rollback previsto. La guía documenta el flujo completo.

## Documentación

- [Baseline académico](docs/BASELINE.md)
- [Alcance del TP](docs/ALCANCE_TP.md)
- [Contribuciones](docs/CONTRIBUCIONES.md)
- [Auditoría técnica vigente](docs/AUDITORIA_TECNICA.md)
- [Roadmap](docs/ROADMAP_PROFESIONALIZACION.md)
- [Seguridad](docs/SEGURIDAD.md)
- [Despliegue, actualización y rollback](docs/GUIA_DESPLIEGUE.md)
- [Migraciones Flyway](docs/GUIA_MIGRACIONES.md)
- [Observabilidad mínima](docs/OBSERVABILIDAD.md)
- [Arquitectura](docs/ARQUITECTURA.md)
- [Modelo de datos](docs/MODELO_DE_DATOS.md)
- [Roles y permisos](docs/ROLES_Y_PERMISOS.md)

## Límites conocidos

- El rate limit de backend vive en memoria y solo coordina una instancia.
- Falta validar el candidato en el Cloud Server y con el proxy/dominio definitivos.
- Una base histórica real requiere adopción Flyway ensayada; las pruebas limpias no reemplazan ese ejercicio.
- No hay alta disponibilidad, WAF, SIEM ni observabilidad distribuida; para el piloto se propone monitoreo externo simple y alertas de host.
- El frontend conserva parte de la orquestación en el componente raíz; no bloquea el piloto, pero limita mantenibilidad futura.

Fuera de alcance de este endurecimiento: nuevas reglas de negocio, pagos parciales, pasarela de cobro, WhatsApp, QR, app móvil y módulo de eventos.
