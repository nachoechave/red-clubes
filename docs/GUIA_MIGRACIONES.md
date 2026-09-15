# Guía de migraciones

## Política

Flyway es la única autoridad para evolucionar el esquema. Hibernate usa `ddl-auto=validate`: valida el mapeo pero nunca crea ni corrige tablas en producción. Las migraciones V1–V6 son publicadas e inmutables; cualquier cambio nuevo se agrega como V7 o superior.

El hardening de producción no necesitó una migración nueva: el costo PBKDF2 viaja dentro del hash y se eleva progresivamente en el login; red, runtime, backups y health no alteran el esquema.

## Instalación nueva

1. Crear MySQL 8.4 vacío con `utf8mb4`/`utf8mb4_0900_ai_ci`.
2. Usar el usuario de aplicación (`MYSQL_USER`), nunca root desde Spring.
3. Mantener `FLYWAY_ENABLED=true` y `FLYWAY_BASELINE_ON_MIGRATE=false`.
4. Iniciar backend y comprobar que Flyway aplica V1–V6.
5. Confirmar versión/checksums en `flyway_schema_history`, `ddl-auto=validate` y `/api/health`.
6. Ejecutar pruebas funcionales, backup y restore aislado.

## Adopción de una base preexistente

No apuntar primero a producción. `baseline` solo registra una versión: no ajusta columnas, no resuelve duplicados y no instala retrospectivamente constraints de V1.

1. Crear dump consistente, checksum y copia externa; probarlo con `ops/restore-test.sh` o un MySQL aislado equivalente.
2. Registrar versión MySQL, charset/collation, zona horaria, tamaño y conteos por tabla.
3. Comparar estructura completa con V1: tipos, nullability, índices, unique y foreign keys.
4. Detectar duplicados club+DNI, club+número de socio, club+socio+actividad, club+socio+período y club+actividad+socio+fecha.
5. Detectar huérfanos y valores que V2/V3 no puedan convertir sin decisión de negocio.
6. Aprobar el saneamiento con dueño funcional; no borrar/combinar automáticamente.
7. Restaurar otra copia y ejecutar el saneamiento versionado/repetible.
8. Solo en esa copia, evaluar `FLYWAY_BASELINE_ON_MIGRATE=true` una vez para marcar V1 si el esquema realmente equivale.
9. Volver a `false`, aplicar V2–V6, comprobar checksums, conteos, constraints e índices.
10. Ejecutar backend/frontend tests, smoke, backup y restore del resultado.
11. Medir duración y definir ventana, bloqueo de escrituras, criterio de aborto, RPO/RTO y rollback.
12. Repetir exactamente en producción con aprobación y evidencia; no improvisar `repair`.

## Migraciones vigentes

- V1: esquema base, relaciones, constraints e índices multi-club.
- V2: historial/actor de inscripciones y estados/actor de asistencia.
- V3: período e importe decimal de cuota, entidad Pago e importación histórica.
- V4: adopción exclusiva de hash de bearer y revocación de sesiones anteriores.
- V5: auditoría e índices de consulta.
- V6: profesor responsable nullable, preservando el texto histórico.

V4 obliga a iniciar sesión nuevamente. V6 no une nombres ambiguos. Esos comportamientos son deliberados.

## Reglas para V7+

- No editar, renombrar ni reordenar un archivo aplicado.
- Nombre `V<secuencia>__descripcion_en_snake_case.sql` y una responsabilidad clara.
- Cambios destructivos en fases: agregar, poblar/verificar, cambiar lectores y retirar después.
- Constraints solo tras consulta diagnóstica y saneamiento aprobado.
- No incluir secretos, datos personales reales ni valores dependientes del host.
- Scripts deterministas; no depender de fecha actual o consultas remotas.
- Probar base limpia MySQL, H2 de tests si aplica y copia histórica representativa.
- Documentar lock/tiempo/espacio, compatibilidad binaria, rollback y verificación posterior.

## Verificación y diagnóstico

```sql
SELECT installed_rank, version, description, type, script, checksum, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

Nunca cambiar un checksum para “hacerlo pasar”. Ante diferencia: detener despliegue, comparar el archivo con el release original y determinar si el ambiente o el repositorio fue alterado. `flyway repair` requiere incidente, backup, revisión y aprobación explícita.

## Integridad existente

El esquema cubre unicidad multi-club de socios, actividades, inscripciones, cuotas y asistencias; asignaciones únicas; sesión/token hash único; foreign keys e índices para estado, período, pago, auditoría y profesor. `DatabaseIntegrityTests` verifica una instalación limpia, pero no reemplaza MySQL real ni el saneamiento histórico.

## Rollback de esquema

Flyway Community aplica migraciones hacia adelante; el rollback soportado es desplegar un binario compatible o restaurar un backup completo validado. Antes de una migración potencialmente incompatible, probar ambas rutas y evitar cambios destructivos en la misma ventana que una gran actualización funcional.
