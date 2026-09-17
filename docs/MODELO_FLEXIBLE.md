# Modelo flexible de clubes

## Auditoría previa

El backend es un monolito Spring Boot/JPA con MySQL y Flyway (V1–V6).
Actividad pertenece a Club; Socio pertenece a Club. InscripcionActividad une
club, socio y actividad con unicidad por los tres IDs, estado y auditoría de bajas.
Asistencia referencia la misma actividad y socio, con unicidad por fecha.
UsuarioClub autoriza roles por institución; UsuarioActividad delimita los grupos
visibles para profesores. El rol global privilegiado se llama SUPERUSUARIO.
Cuota y Pago dependen del socio/club, no del tipo de actividad.

GestionService concentra grupos, inscripciones y asistencia; ReporteService reutiliza
sus respuestas. Los controladores autorizan el club y los servicios validan sus
relaciones. Spring Security autentica por sesión opaca y deniega por defecto.

Contratos existentes: /api/auth (login, me, logout, cambiar-password), /api/usuarios
(CRUD y asignaciones), /api/clubes (alta/edición/listado), y bajo cada club:
socios, actividades, socios/{id}/actividades, actividades/{id}/asistencias/{fecha},
dashboard, reportes, cuotas (generación, pagos, anulación) y auditoria.

Angular utiliza servicios HTTP por dominio, pero la orquestación y las pantallas
residen principalmente en app.ts/app.html. ActividadVista se usa en formularios,
asignación de profesores, inscripciones, perfiles de participantes, dashboard,
reportes y asistencia. No conviene duplicar estas pantallas por institución.

Pruebas previas: integración de seguridad y roles, integridad SQL con H2/Flyway,
servicios de gestión/cobranza/reportes/socios/usuarios, sesiones, passwords,
bootstrap y auditoría. Frontend: componente raíz, servicios y autenticación;
baseline frontend ejecutado: 13 pruebas correctas.

## Decisión de migración

Ampliar Actividad como entidad de grupo conservando nombre Java, tabla e IDs.
TipoGrupo distingue ACTIVIDAD, TALLER y CATEGORIA_DEPORTIVA; no hay una entidad
ni inscripción distinta por tipo. Socio sigue siendo la identidad del participante.
La API genérica reutiliza los mismos servicios y las rutas históricas permanecen.
Los nombres internos históricos se documentan como deuda de nomenclatura.

La migración será aditiva para tipos, asistencia, disciplina y configuración.
Se flexibilizan columnas específicas sin eliminar datos. Los registros existentes
quedan como ACTIVIDAD y asistencia habilitada. Los clubes conservan los módulos
históricos; categorías y talleres se habilitan explícitamente, sin un TipoClub rígido.
La configuración no elimina registros al deshabilitar un módulo.

Riesgos a verificar: referencias cruzadas de club, profesores sin asignación,
inscripciones conservadas al editar, cupo sin límite, asistencia deshabilitada,
acceso directo a pantallas ocultas y actualización V6→V7 con datos relacionados.
No se modifican migraciones previas, autenticación, cuotas o pagos.

## Implementación y contratos

- Entidades ampliadas: `Actividad` y `Club`. No se agregan entidades paralelas de
  categoría, taller o inscripción. `club_modulo` es una colección de valores JPA
  con clave `(club_id, modulo)`, respaldada por `ModuloClub`.
- Nuevos tipos: `TipoGrupo`, `ModuloClub`, `NombreParticipantes`;
  DTOs `GuardarGrupoRequest`, `ConfiguracionClubRequest` e inscripción por `grupoIds`.
- `V7__grupos_y_modulos_por_club.sql` agrega tipo, disciplina, año/categoría,
  asistencia y módulos; permite NULL en profesor, días, categoría, icono y cupo.
  NULL en cupo significa sin límite. Las respuestas mantienen cadenas vacías e
  icono de reserva para facilitar compatibilidad de clientes históricos.
- Responsables opcionales: cualquier usuario activo vinculado al mismo club.
  Elegirlo no concede roles ni acceso automáticamente; se conservan las asignaciones
  explícitas de `UsuarioActividad` para acceso de profesores.
- Sin calendario, un grupo con asistencia habilitada admite cualquier día.
  Con calendario, sigue validando el día. Los profesores siguen limitados a la
  fecha actual de Argentina. Club y grupo deben permitir asistencia, tanto al
  consultar como al guardar, incluidas las rutas antiguas.

Rutas nuevas, relativas a `/api/clubes/{clubId}`:

| Método | Ruta | Uso |
|---|---|---|
| GET | `/grupos?tipo=...` | Todos los grupos autorizados, con filtro opcional |
| POST | `/grupos` | Alta de cualquiera de los tres tipos |
| PUT | `/grupos/{grupoId}` | Edición de grupo con validación de pertenencia |
| GET / PUT | `/socios/{socioId}/grupos` | Inscripciones comunes; PUT recibe grupoIds |
| GET / POST | `/grupos/{grupoId}/asistencias/{fecha}` | Asistencia opcional |
| GET | `/reportes/grupos?tipo=...` | Grupos y cantidades de inscripciones activas |
| PUT | `/configuracion` | Módulos y nombre de participantes del club |

Se mantienen todas las rutas anteriores. La lectura histórica de `actividades`
y las inscripciones históricas filtran ACTIVIDAD. El PUT histórico conserva las
inscripciones a categorías y talleres aunque el cliente no las envíe; no permite
modificar esos vínculos. La API genérica representa la colección completa. El alta histórica
crea ACTIVIDAD con sus validaciones anteriores. La edición histórica solo edita
ACTIVIDAD; para tipos nuevos se usa `/grupos`. El PUT de inscripciones es un
reemplazo de la selección completa, no un agregado individual.

La configuración es funcional, no reemplaza autorización. Los módulos controlan
navegación, formularios, indicadores y cargas del frontend; el backend también
valida el módulo del tipo al crear/editar grupos y exige asistencia habilitada.
Las consultas históricas siguen disponibles a roles autorizados, incluso cuando
se oculta un módulo. Esto permite conservar y consultar datos sin borrarlos.

## Uso desde Angular

En Configuración, el administrador del club o SUPERUSUARIO elige los módulos y
Socios/Jugadores/Alumnos/Participantes. El menú muestra Categorías, Actividades o
Talleres cuando solo hay un tipo habilitado; en clubes mixtos muestra Grupos con
filtro de tipo, reutilizando la misma pantalla. Usuarios y roles sigue disponible
según permisos: deshabilitar profesores no impide administrar operadores.

El alta y edición seleccionan el tipo. Categorías muestra nombre, disciplina,
año/categoría, responsable opcional si está habilitado el módulo y asistencia
opcional. Oculta calendario, cupo e icono. Actividades/talleres muestran sus campos
habituales y responsable opcional. Desde cada grupo se abre su lista filtrada de
participantes. La inscripción se gestiona desde la ficha existente de la persona.
Los reportes incluyen cantidades actuales por grupo; no representan una foto
histórica de inscripciones al cierre del rango de fechas.

La navegación impide abrir módulos deshabilitados por URL y oculta cuotas y
asistencia en indicadores secundarios. No se reemplazan autenticación, sesiones,
formularios personales, cuotas ni pagos.

## Validación y límites

Baseline ejecutado: 44 tests backend y 13 frontend correctos. Se agregaron
`GruposIntegrationTests`, `GruposMigrationTests` y `flexible-club.spec.ts`.
Cubren tipos sin campos obligatorios, convivencia, profesores/responsables,
inscripciones, límites de cupo, asistencia habilitada/deshabilitada, compatibilidad,
IDOR en grupos/participantes/asistencias, roles, configuración y controles Angular.
La prueba de migración crea V6 con datos relacionados, ejecuta V7 y comprueba
IDs, profesor, inscripciones, asistencia y asignaciones, además de módulos iniciales.

La migración automatizada corre sobre H2 en modo MySQL, como las pruebas previas.
No se aplicó contra una base productiva. Docker no estaba iniciado en este equipo;
queda pendiente ensayar V7 sobre una copia MySQL 8.4 con datos de destino antes
del despliegue, incluyendo duración de ALTER TABLE y backup restaurable.

Deuda restante: nombres internos Actividad/Socio, orquestación extensa en App,
consultas por grupo para contar inscripciones, control de cupos concurrentes
heredado y bundle inicial por encima del umbral informativo de 500 kB.
Segunda etapa: separar componentes Angular y carga diferida, agrupar consultas de
reportes, bloquear operaciones concurrentes de cupo y diseñar reportes históricos
de inscripciones. Un renombrado físico de tablas puede esperar a una API versionada.

## Resultado ejecutado (17 de septiembre de 2026)

| Verificación | Resultado |
|---|---|
| Java 21, `mvnw.cmd verify` | BUILD SUCCESS; JAR ejecutable generado |
| Backend | 62 tests, 0 fallos, 0 errores, 0 omitidos (18 nuevos) |
| Node 22.23.2, `npm test -- --watch=false` | 25 tests correctos en 6 archivos (12 nuevos) |
| `npm run build` | Correcto; bundle inicial 567,03 kB, transferencia estimada 117,22 kB |
| Presupuesto Angular | Advertencia de 500 kB; no supera el límite de error de 1 MB |
| `git diff --check` | Sin errores de whitespace |
| MySQL real / despliegue | No ejecutado; Docker no estaba iniciado |

Branch: `feature/flexible-club-model`. Se preservaron fuera de los commits todos
los cambios locales previos de preparación del piloto, incluido su test de bootstrap.
No se hizo push, despliegue ni modificación de una base productiva.
