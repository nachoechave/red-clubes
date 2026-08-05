# Auditoría técnica de Red Clubes

Fecha de corte: 5 de agosto de 2026
Rama inspeccionada: `feature/reglas-profesores-asistencia-cuotas`
Base Git: `7ea4d3f`
Alcance: repositorio local, incluidos los cambios sin confirmar existentes al inicio de la auditoría.

## Resumen ejecutivo

Red Clubes es un prototipo funcional de monolito web con Angular 21, Java 21, Spring Boot 3.5 y MySQL. El flujo principal tiene avances reales en login, selección de club, socios, actividades, inscripciones, cuotas, asistencia, dashboard, reportes y usuarios. Sin embargo, todavía no cumple la definición de un MVP desplegable: la seguridad estaba aplicada de forma manual y desigual, existían rutas legacy públicas, había credenciales versionadas, faltaban migraciones, los tests no eran reproducibles y el frontend concentra toda la aplicación en un solo componente.

La recomendación arquitectónica es conservar el monolito y la sesión opaca persistida en servidor durante el MVP. No hay una razón técnica para migrar a JWT ahora. Sí es necesario centralizar progresivamente autenticación y autorización, endurecer el aislamiento por club y completar integridad de base de datos antes de ampliar funcionalidades.

## Evidencia de compilación y pruebas iniciales

| Verificación | Resultado inicial | Diagnóstico |
|---|---|---|
| `backend/mvnw.cmd clean verify` | Falló | El código principal compiló (75 archivos), pero el único `@SpringBootTest` intentó conectarse a MySQL local. No había perfil ni base aislada de test. |
| `frontend/npm test -- --watch=false` | Falló | 1 prueba pasó y 1 falló. La expectativa generada buscaba `Hello, red-clubes-frontend`, mientras la UI real muestra `Red Clubes`. |
| `frontend/npm run build` | Falló | El proceso nativo de Angular terminó con código `-1073741819` durante `Building...`, sin diagnóstico de TypeScript. Requiere aislar el fallo del builder/entorno. |
| Inspección de secretos | Crítica | `application.properties` contenía usuario MySQL `root` y una contraseña; el inicializador creaba un superusuario con DNI y contraseña predecibles. |

## Estado funcional

### Funcionalidades completas para prototipo

- Login por DNI y contraseña con PBKDF2, salt aleatorio y comparación resistente a diferencias temporales simples.
- Sesiones opacas persistidas con expiración.
- Selección de clubes asignados al usuario.
- CRUD parcial de socios por club.
- Alta y edición de actividades.
- Asignación de profesores a actividades.
- Inscripciones básicas socio-actividad.
- Listado y marcado de asistencia sobre inscripciones activas.
- Listado de cuotas y registro básico de pago.
- Dashboard y reportes básicos.
- Alta, activación, desactivación y asignaciones de usuarios.

Estas capacidades se consideran completas solo a nivel de prototipo; varias carecen de todas las restricciones, DTOs, auditoría y pruebas necesarias para producción.

### Funcionalidades parciales

- Inscripciones: existen entidad y persistencia, pero faltan todos los metadatos solicitados, auditoría y restricciones de base; el contador de inscriptos sigue almacenado.
- Cuotas: mezclan obligación y pago en la misma entidad; no existe entidad `Pago`, generación mensual ni anulación.
- Asistencia: usa booleano presente; faltan estados justificado/ausente explícitos y usuario responsable.
- Dashboard: no acepta período; mezcla métricas históricas y mensuales y usa meses enero-julio hardcodeados.
- Usuarios y permisos: existe rol global y rol por club, pero el modelo necesita una matriz formal y pruebas exhaustivas.
- Manejo de errores: existe `@RestControllerAdvice`, pero el contrato no es uniforme ni incluye timestamp/código estable.
- CORS: estaba fijo para dos orígenes locales.

### Funcionalidades faltantes

- Pagos como entidad e historial auditable.
- Generación y vencimiento mensual de cuotas.
- Eventos.
- Auditoría de operaciones sensibles.
- Filtros reales de dashboard y reportes.
- Rutas Angular, guards, interceptor y servicios por feature.
- Adopción y saneamiento de la base MySQL preexistente; las instalaciones nuevas ya cuentan con baseline Flyway y restricciones versionadas.
- Dockerfiles, Compose reproducible y documentación de despliegue.
- CI completa.
- Paginación y estrategia de consultas para volumen real.

## Cinco problemas más críticos encontrados

1. **Credenciales y cuenta inicial predecible (crítico).** La configuración versionaba acceso MySQL `root` y el bootstrap creaba una cuenta privilegiada cuya contraseña era igual al DNI.
2. **Endpoints legacy públicos (crítico).** `/api/socios`, `/api/socios/{id}` y sus variantes de escritura no exigían autenticación y exponían entidades completas.
3. **IDOR entre clubes (crítico).** Actualizar/eliminar socios y registrar pagos autorizaba el `clubId` de la URL, pero luego cargaba el recurso solo por su ID. Un administrador podía combinar el club autorizado con el ID de otro club.
4. **Administración de usuarios global desde un club (crítico).** Un administrador podía listar todos los usuarios y la creación omitía validar que las asignaciones pertenecieran a clubes administrados por quien hacía la operación.
5. **Base y pruebas no reproducibles (alto).** `ddl-auto=update`, ausencia de Flyway y tests dependientes de MySQL local impiden validar cambios con seguridad y desplegar el mismo esquema en cada entorno.

## Seguridad

### Autenticación

- La elección de sesión opaca es apropiada para este MVP: permite revocación inmediata y evita complejidad innecesaria de JWT.
- El token se almacena en texto claro en base de datos; conviene migrar a hash de token y agregar índice/único.
- Al inicio no existía logout y un token inválido se trataba como `403` en lugar de `401`.
- No hay límite de intentos ni bloqueo temporal de login.
- La autorización está repetida en controladores. Mientras no se incorpore Spring Security o un interceptor central, cada endpoint nuevo es propenso a quedar público.

### Autorización multi-club

- Las consultas agregadas filtran mayormente por club.
- Se detectaron búsquedas peligrosas por ID en socios, cuotas, usuarios, actividades y asistencias.
- Actividades y asistencias contienen algunas comprobaciones cruzadas correctas introducidas en los cambios locales.
- La gestión global de usuarios necesita tratar especialmente usuarios con asignaciones en múltiples clubes: una desactivación global no debe quedar en manos de un administrador parcial.

### Datos sensibles y logs

- No se observaron tokens impresos explícitamente en logs.
- `show-sql=true` puede revelar datos y parámetros operativos; debe permanecer apagado fuera de diagnóstico local.
- Los errores deben evitar mensajes internos en producción.

## Integridad y modelo de datos

- Al inicio no existían migraciones versionadas. La fase 2 incorporó `V1__baseline_schema.sql` para instalaciones nuevas; la base MySQL preexistente aún requiere inventario, backup y adopción controlada.
- El baseline nuevo declara restricciones únicas para club+DNI, club+socio+actividad, club+socio+período y club+actividad+socio+fecha. Todavía deben auditarse duplicados antes de imponerlas sobre datos existentes.
- Varias relaciones `@ManyToOne` aceptan `null` aunque el dominio las requiere.
- `Socio.estado` es texto libre; debe ser enum.
- `Actividad.inscriptos` duplica un dato derivable y puede desincronizarse.
- `Cuota.mes` es texto y no modela período de forma inequívoca.
- `Cuota.importe` es entero; para moneda conviene `BigDecimal` con escala explícita.
- Pago no es una entidad, por lo que no existe historial ni anulación segura.
- Los controladores devuelven entidades JPA en socios, con acoplamiento de persistencia y riesgo de exposición accidental.
- Completar metadatos de socios durante una lectura escribe en base de datos, lo que sorprende al consumidor y dificulta transacciones.

## Arquitectura y mantenibilidad

### Backend

- La separación por paquetes (`clubes`, `socios`, `gestion`, `usuarios`) es una base válida de monolito modular.
- `GestionService` reúne actividades, inscripciones, cuotas, asistencia, dashboard y reportes; debe dividirse por caso de uso.
- Parte de las reglas está en controladores y parte en servicios.
- Faltan DTOs para socios y varios comandos.
- Existen consultas en memoria y N+1 previsibles al mapear relaciones y métricas.

### Frontend

- `App` tiene aproximadamente 1.600 líneas, la plantilla 1.340 y los estilos 2.050.
- No hay rutas Angular (`routes = []`).
- No hay servicios HTTP por dominio, guard ni interceptor.
- La URL `http://127.0.0.1:8080/api` está hardcodeada.
- El token y el estado de sesión se concentran en el componente; la recarga no restaura una sesión de forma profesional.
- Se usa principalmente template-driven forms; los flujos críticos deberían migrar a Reactive Forms al extraer cada feature.

## Experiencia de usuario

- La interfaz cubre muchos flujos en una sola pantalla y ofrece estados visuales, pero la navegación no es direccionable ni recuperable por URL.
- No se comprobó accesibilidad completa por teclado o lector de pantalla.
- La gestión de errores depende de lógica repetida del componente.
- La ausencia de paginación puede degradar la UI al crecer socios, cuotas o usuarios.

## Infraestructura y despliegue

- No hay Dockerfiles ni Compose.
- No hay perfiles completos de desarrollo/producción ni health check estándar de Actuator.
- No hay estrategia de backups documentada.
- No hay workflow CI utilizable en el inventario actual.
- Los scripts `.cmd` asumen configuración local.
- La raíz README describe el proyecto como inicial y no permite reproducir el entorno.

## Correcciones iniciadas en la fase 1

- Externalización de base de datos, CORS, duración de sesión y bootstrap.
- Desactivación por defecto de la creación automática de superusuario.
- Perfil H2 aislado para tests.
- Respuesta `401` para ausencia o invalidez de credenciales y logout real.
- Retiro de endpoints legacy públicos de socios.
- Carga de socio y cuota por ID más club.
- Restricción de operaciones administrativas de usuarios al alcance real del actor.

## Verificación posterior al hardening y la fase 2

| Verificación | Resultado |
|---|---|
| `backend/mvnw.cmd clean verify` | 30 pruebas, 0 fallos, 0 errores. Incluye seis migraciones, Spring Security, hash de bearer, límite de login, auditoría, aislamiento multi-club, constraints, inscripción/asistencia, pagos y reportes. |
| `frontend/npm install` | Dependencias actualizadas; lockfile consistente (`up to date`). |
| `frontend/npm test -- --watch=false` | 12 pruebas, 12 aprobadas. Incluye persistencia de sesión, interceptor bearer/401/403, guards de sesión/rol y servicios de features. |
| `frontend/npx ngc -p tsconfig.app.json` | Compilación Angular/TypeScript aprobada. |
| `frontend/npm run build` con Node 22.23.2 | Build de producción aprobado; bundle inicial 447,13 kB y transferencia estimada 103,05 kB. |

En este equipo, el mismo builder terminó con la excepción nativa Windows `0xC0000005` bajo Node 24. La compilación fue reproducible bajo Node 22.23.2, por lo que se añadió `.nvmrc`, restricción `engines` y una validación `prebuild`. No fue necesario cambiar versiones mayores de Angular.

## Riesgos que permanecen

- La contraseña ya expuesta debe considerarse comprometida y rotarse en cualquier base donde se haya usado; eliminarla del último commit no borra el historial Git.
- La base MySQL real no estaba disponible durante la auditoría; faltan dump, conteos, detección de duplicados y ensayo de rollback antes de adoptar Flyway.
- Los constraints están probados en una base nueva H2 compatible, pero aún no aplicados ni verificados sobre MySQL real.
- El hash de tokens, el rate limit de login y la auditoría ya están implementados; resta observarlos bajo carga real y ajustar sus umbrales operativos.
- El frontend sigue monolítico y con URL hardcodeada.
- El build de producción exige Node 22 hasta resolver o descartar el fallo nativo observado con Node 24.

## Orden recomendado

1. Cerrar y verificar el hardening crítico y las pruebas multi-club.
2. Diseñar baseline Flyway no destructivo sobre una copia de datos y añadir constraints.
3. Formalizar roles/permisos y centralizar seguridad.
4. Completar inscripciones sin contador manual.
5. Separar cuota y pago, con auditoría.
6. Completar asistencia y trazabilidad.
7. Corregir métricas y filtros.
8. Extraer el frontend por features, comenzando por auth y socios.
9. Incorporar contenedores, CI y documentación de despliegue.
