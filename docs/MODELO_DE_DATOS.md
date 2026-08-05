# Modelo de datos

Estado del esquema: Flyway V1–V6, verificado en H2 compatible con MySQL el 5 de agosto de 2026.

## Entidades y relaciones

| Entidad | Propósito | Relaciones principales |
|---|---|---|
| `Club` | Institución gestionada | socios, actividades, cuotas, pagos, usuarios y auditoría |
| `Usuario` | Persona que inicia sesión | asignaciones a clubes/actividades, sesiones y auditoría |
| `SesionUsuario` | Sesión opaca con vencimiento | usuario; columna `token` contiene SHA-256, no el bearer |
| `UsuarioClub` | Rol de un usuario dentro de un club | usuario + club + rol |
| `UsuarioActividad` | Actividad habilitada a un profesor | usuario + club + actividad |
| `Socio` | Persona asociada a un club | club, inscripciones, cuotas y asistencias |
| `Actividad` | Taller o clase | club, profesor usuario opcional, inscripciones y asistencias |
| `InscripcionActividad` | Vínculo socio–actividad con historial | club, socio, actividad y usuario responsable |
| `Asistencia` | Estado de un inscripto en una fecha | club, actividad, socio y usuario responsable |
| `Cuota` | Obligación de pago mensual | club, socio y pagos |
| `Pago` | Operación que cancela una cuota | club, cuota, responsables y estado de anulación |
| `Auditoria` | Registro de mutaciones sensibles | actor, club, acción, tipo e ID de entidad |

## Reglas de integridad relevantes

- `Socio`: DNI y número de socio únicos dentro del club.
- `InscripcionActividad`: una fila por club, socio y actividad; las bajas se conservan y pueden reactivarse.
- `Asistencia`: una fila por club, actividad, socio y fecha.
- `Cuota`: una fila por club, socio y período `YYYY-MM`; importe monetario en `DECIMAL(12,2)`.
- `Pago`: solo un pago activo por cuota se garantiza en servicio bajo bloqueo pesimista; el historial anulado no se elimina.
- `Actividad`: el profesor nuevo se referencia por `profesor_usuario_id`; el texto anterior se conserva para datos históricos no vinculados.
- Todas las relaciones operativas verifican que club, socio y actividad coincidan antes de guardar.

## Estados

| Entidad | Estados |
|---|---|
| Club | `ACTIVO`, `INACTIVO` |
| Usuario | `ACTIVO`, `INACTIVO` |
| Socio | `ACTIVO`, `INACTIVO` |
| Actividad | `ACTIVA`, `INACTIVA` |
| Inscripción | `ACTIVA`, `BAJA` |
| Asistencia | `PRESENTE`, `AUSENTE`, `JUSTIFICADO` |
| Cuota | `PENDIENTE`, `PAGADA`, `VENCIDA`, `ANULADA` |
| Pago | `ACTIVO`, `ANULADO` |

## Cuotas y pagos

`Cuota` representa la deuda; `Pago` representa la operación. Registrar un pago activo pasa la cuota a `PAGADA`. Anularlo conserva la fila, fecha, motivo y responsables, y devuelve la cuota a `PENDIENTE` o `VENCIDA`. El MVP acepta pago total únicamente; los pagos parciales quedan fuera de alcance.

La recaudación se calcula desde `Pago.fechaPago` y `Pago.estado=ACTIVO`, no desde el vencimiento ni desde el estado aislado de la cuota. Los pagos históricos importados sin fecha no se atribuyen artificialmente a un mes.

## Auditoría

Cada registro incluye usuario, club cuando aplica, acción, tipo e ID de entidad, fecha de Buenos Aires y un detalle limitado a 1000 caracteres. No se guardan contraseñas, hashes, bearers ni cuerpos completos de requests.

## Migraciones

- V1: esquema base, claves, constraints e índices.
- V2: inscripción y asistencia auditables.
- V3: período/importe de cuota y entidad Pago.
- V4: revocación controlada de sesiones al adoptar hashes.
- V5: tabla e índices de auditoría.
- V6: relación nullable entre actividad y usuario profesor.

Antes de ejecutar estas migraciones sobre una base existente se debe seguir `GUIA_MIGRACIONES.md`, tomar un backup y probar la restauración.
