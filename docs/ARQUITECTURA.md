# Arquitectura de Red Clubes

Estado verificado: 5 de agosto de 2026.

## Enfoque

Red Clubes es un monolito modular. Angular entrega la interfaz, Spring Boot concentra API, seguridad y reglas de negocio, y MySQL conserva los datos. No hay microservicios ni dependencias de un proveedor cloud. Esta forma es apropiada para un piloto pequeño porque reduce costo operativo y sigue permitiendo mover el sistema entre un Cloud Server y la infraestructura municipal.

```text
Navegador
  -> HTTPS /
  -> Nginx (Angular + headers + limite de login)
       /api/** -> Spring Boot 3.5 / Java 21
                    -> MySQL 8.4
```

En Compose solo Nginx publica un puerto. Backend y MySQL permanecen en la red privada. Angular consume `/api`, de modo que el dominio piloto puede reemplazarse más adelante por un subdominio de `ensenada.gov.ar` sin recompilar URLs.

## Backend

Los paquetes separan responsabilidades:

- `config`: Spring Security, CORS y respuestas globales de error.
- `usuarios`: autenticación, sesiones opacas, roles y asignaciones.
- `clubes`: administración de instituciones.
- `socios`: altas, modificaciones y bajas lógicas.
- `gestion`: actividades, inscripciones, asistencia, cuotas, pagos y reportes.
- `auditoria`: trazabilidad de operaciones sensibles.

Los controladores reciben y devuelven DTOs. Los servicios aplican reglas y transacciones. Los repositorios siempre incluyen `clubId` cuando el recurso tiene alcance por club. Flyway es la única fuente de cambios de esquema y producción usa `ddl-auto=validate`.

La autenticación usa un bearer opaco de 256 bits. La base conserva únicamente SHA-256 del bearer; una filtración de la tabla de sesiones no entrega credenciales reutilizables. Las sesiones vencidas se purgan y el login se limita por una clave derivada de identidad e IP.

## Frontend

Angular 21 usa componentes standalone, signals y rutas reales protegidas. `core/auth` contiene store de sesión, servicio, interceptor y guards. La API usa el origen relativo configurado en `core/config` y el proxy local de Angular.

La extracción a componentes y servicios por feature es incremental: autenticación y routing ya están separados, pero parte de la orquestación de socios, actividades, cuotas, asistencia y reportes aún vive en `App`. Es una limitación de mantenibilidad conocida; no afecta el aislamiento de seguridad, que se aplica nuevamente en el backend.

## Flujos principales

1. Login entrega un bearer una sola vez y Angular lo conserva en `sessionStorage` de la pestaña.
2. El interceptor agrega el bearer solo a `/api` y limpia la sesión ante `401`.
3. El usuario selecciona un club dentro de sus asignaciones.
4. Cada endpoint valida identidad, rol, club y, cuando corresponde, actividad.
5. Una mutación sensible guarda negocio y auditoría en la misma transacción.
6. Dashboard y reportes calculan recaudación desde pagos activos y asistencia por período real.

## Decisiones operativas

- MySQL no se publica a Internet.
- TLS termina en el proxy público del servidor; Nginx de la app conserva headers defensivos.
- Los secretos llegan por variables de entorno y `.env` nunca se versiona.
- El despliegue produce imágenes reproducibles y CI ejecuta backend, frontend y validación de Compose.
- Los backups son dumps consistentes comprimidos y deben copiarse fuera del servidor.

## Limitaciones arquitectónicas conocidas

- Compose todavía debe construirse y probarse en un host con Docker Linux disponible.
- La adopción de una base MySQL existente exige inventario, dump y ensayo previo; H2 valida migraciones, pero no sustituye esa prueba.
- El almacenamiento en memoria del límite de login es suficiente para una instancia piloto; varias réplicas requerirían un contador compartido.
- El contador legado `actividad.inscriptos` permanece por compatibilidad de esquema, pero las respuestas calculan inscriptos activos desde las inscripciones.
- Eventos y cumpleaños no forman parte del núcleo cerrado en esta fase.
