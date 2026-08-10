# Roles y permisos

La autorización combina rol global, asignación por club y, para profesores, asignación por actividad. Cambiar un ID en una URL no amplía el alcance.

| Operación | Superusuario | Administrador del club | Operador del club | Profesor asignado |
|---|:---:|:---:|:---:|:---:|
| Administrar clubes | Sí | No | No | No |
| Ver socios del club | Sí | Sí | Sí | No |
| Alta, edición o baja de socios | Sí | Sí | Sí | No |
| Crear o editar actividades | Sí | Sí | No | No |
| Ver actividades | Todas | Todas las del club | Todas las del club | Solo asignadas |
| Administrar inscripciones | Sí | Sí | Sí | No |
| Generar cuotas y registrar/anular pagos | Sí | Sí | Sí | No |
| Ver cuotas y reportes | Sí | Sí | Sí | No |
| Tomar asistencia | Sí | Sí, cualquier fecha válida | Sí, cualquier fecha válida | Solo actividad asignada y día actual de clase |
| Administrar usuarios | Global | Solo usuarios completamente dentro de sus clubes | No | No |
| Crear otro superusuario | Sí | No | No | No |
| Consultar auditoría del club | Sí | Sí | No | No |

## Reglas adicionales

- Un administrador local no puede asignar administradores ni tocar usuarios que también pertenecen a clubes fuera de su control.
- Un administrador local puede crear operadores y profesores únicamente dentro de sus clubes. Solo el superusuario puede crear administradores.
- El rol global y el rol por club deben coincidir; no se aceptan asignaciones incoherentes.
- Un operador no puede administrar clubes, usuarios, actividades ni auditoría, aunque conozca las URLs de esos endpoints.
- Una actividad solo acepta como responsable a un usuario activo, con rol global `PROFESOR` y rol `PROFESOR` en ese club.
- Un profesor no puede guardar asistencia para una actividad no asignada, una fecha distinta del día actual ni un día fuera del horario configurado.
- Toda mutación vuelve a validar pertenencia de recursos en el backend, aunque la interfaz o el guard oculten la opción.
- Falta de sesión produce `401`; sesión válida sin permiso produce `403`; una regla de negocio inválida produce `400`.
