# Seguridad

Estado verificado: 5 de agosto de 2026.

## Autenticación

Spring Security protege todo `/api/**` salvo `GET /api/health`, `POST /api/auth/login` y preflight `OPTIONS`. La política HTTP es stateless: la credencial viaja como `Authorization: Bearer` y no se crea una sesión de servidor adicional.

El bearer tiene 256 bits aleatorios. Solo se devuelve al iniciar sesión; la tabla conserva SHA-256, por lo que un acceso de lectura a la base no permite reutilizarlo. La sesión tiene vencimiento configurable, logout elimina su hash y una tarea horaria purga expiradas. Flyway V4 revoca sesiones anteriores durante la transición.

Las contraseñas se guardan mediante PBKDF2 con salt y parámetros definidos en `PasswordService`. El bootstrap privilegiado está apagado por defecto y exige variables explícitas.

## Protección del login

La aplicación cuenta fallos por una clave SHA-256 derivada de identidad normalizada e IP. Por defecto permite cinco intentos dentro de quince minutos y bloquea quince minutos; bloqueado y credencial incorrecta producen el mismo `401`, evitando enumerar usuarios. Nginx aplica además un límite por IP.

El contador vive en memoria y es adecuado para una instancia. Si se agregan réplicas, se debe migrar a un almacenamiento compartido. El backend solo debe ser accesible desde el proxy confiable para que los headers de IP no puedan falsificarse directamente.

## Autorización y aislamiento

El filtro carga al usuario activo y Spring establece su rol global. Cada servicio vuelve a verificar rol por club y pertenencia de socio, actividad, inscripción, cuota, pago y asistencia. Los profesores requieren asignación explícita a la actividad. La matriz está en `ROLES_Y_PERMISOS.md`.

- Sin bearer válido: `401 AUTHENTICATION_REQUIRED`.
- Bearer válido sin permiso: `403 ACCESS_DENIED`.
- Regla de negocio inválida: `400 BUSINESS_RULE_VIOLATION`.
- Conflicto de integridad: `409 DATA_INTEGRITY_VIOLATION`.

## Auditoría

Altas/modificaciones/bajas de socios, actividades, inscripciones, asistencia, cuotas, pagos y permisos generan un registro con actor, club, acción, entidad, ID, fecha y detalle acotado. Los administradores pueden consultar los últimos cien registros de su club. No se registran contraseñas, hashes, bearer ni cuerpos completos.

## Navegador y proxy

Angular usa `/api`; el interceptor nunca adjunta el bearer a dominios externos. El token se guarda en `sessionStorage`, se restaura con `/auth/me` y se elimina ante `401`. Las rutas tienen guard de sesión y rol, pero el backend sigue siendo la autoridad.

Nginx oculta versión, limita cuerpo, agrega CSP, `nosniff`, denegación de framing, política de referrer/permisos y HSTS. HSTS solo tiene efecto cuando el proxy público entrega HTTPS. TLS y redirección HTTP→HTTPS deben configurarse en el Cloud Server.

CSRF está desactivado porque el bearer no viaja en una cookie automática. Si se migra a cookies, debe revaluarse junto con `SameSite`, `Secure`, `HttpOnly` y protección CSRF.

## Secretos y datos

- `.env` y backups están ignorados por Git.
- MySQL de aplicación usa un usuario no root; root queda reservado para operación del contenedor y backups.
- Backend y base no exponen puertos en Compose.
- Producción usa mensajes de error mínimos y logs estructurados.
- Backups con datos deben cifrarse al salir del host y probarse mediante restauración.

## Antes de exponer el piloto

1. Rotar cualquier secreto usado durante desarrollo.
2. Probar Compose y migraciones V1–V6 con MySQL 8.4.
3. Verificar TLS, renovación automática y DNS.
4. Ejecutar pruebas de aislamiento con dos clubes y usuarios reales de prueba.
5. Comprobar backup externo y restauración.
6. Revisar permisos del host, firewall, actualizaciones y acceso SSH.
7. Definir retención, incidentes y tratamiento de datos personales con el responsable del piloto.

## Verificación automatizada

Las suites cubren 401/403, hash de bearer, constraints, aislamiento entre clubes, cupo/estado, asistencia, cuotas/pagos, reportes, guards e interceptor. Ejecutar backend `clean verify` y frontend test/build antes de cada publicación.
