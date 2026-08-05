# Guía de migraciones

## Estado

Flyway es la única autoridad para evolucionar el esquema. Hibernate usa `ddl-auto=validate`: verifica mapeos, pero no crea ni modifica tablas. V1–V6 están verificadas sobre una base limpia H2 en modo MySQL; todavía deben ensayarse contra MySQL 8.4 y contra una copia representativa de cualquier base preexistente.

## Base nueva

1. Crear una base vacía con `utf8mb4`.
2. Configurar `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` con privilegios mínimos de aplicación/migración.
3. Mantener `FLYWAY_ENABLED=true` y `FLYWAY_BASELINE_ON_MIGRATE=false`.
4. Iniciar backend; Flyway aplica V1–V6 y Hibernate valida.
5. Confirmar `flyway_schema_history`, health y smoke tests.

## Base preexistente

No apuntar primero a producción. Un baseline marca una versión, pero no corrige diferencias ni instala retrospectivamente constraints de V1.

1. Tomar un dump consistente y restaurarlo en MySQL aislado de la misma versión.
2. Registrar versión, charset/collation, tamaño y conteos por tabla.
3. Comparar columnas, tipos, índices y claves contra V1.
4. Buscar duplicados de club+DNI, club+número de socio, club+socio+actividad, club+socio+período y club+actividad+socio+fecha.
5. Buscar relaciones huérfanas y valores de mes que V3 no pueda convertir a `YYYY-MM`.
6. Aprobar un saneamiento de negocio; no borrar duplicados automáticamente.
7. Solo en la copia, habilitar `FLYWAY_BASELINE_ON_MIGRATE=true` una vez si realmente corresponde marcar V1.
8. Volver inmediatamente a `false`, aplicar migraciones siguientes y comparar conteos.
9. Ejecutar tests, smoke tests, backup y restore de ensayo.
10. Programar producción con ventana, responsables, backup externo y criterio de abortar.

## Migraciones vigentes

- V1: esquema base, relaciones, constraints e índices.
- V2: inscripción con historial/actor y asistencia con estados/actor.
- V3: período e importe decimal de cuota, pago e importación histórica.
- V4: revoca sesiones efímeras para adoptar almacenamiento exclusivo del hash del bearer.
- V5: tabla e índices de auditoría.
- V6: `profesor_usuario_id` nullable, conservando texto histórico.

Los pagos históricos sin fecha no se atribuyen a un mes inventado. V4 obliga a iniciar sesión otra vez. V6 no intenta unir automáticamente nombres ambiguos de profesor.

## Reglas para cambios nuevos

- No editar una migración aplicada; agregar una versión.
- Para cambios destructivos: agregar, rellenar/verificar, cambiar lectores y retirar en una versión posterior con aprobación.
- No incluir secretos ni datos personales reales.
- Incorporar constraints solo después de consultas diagnósticas.
- Probar base limpia y copia histórica.
- Mantener saneamientos que exigen decisión humana fuera de la migración automática.

## Integridad cubierta

- DNI y número de socio únicos por club.
- Nombre de actividad único por club.
- Inscripción única por club+socio+actividad.
- Cuota única por club+socio+período.
- Asistencia única por club+actividad+socio+fecha.
- Asignaciones de usuario no duplicadas.
- Hash de sesión único.
- Índices de pago, período, auditoría y profesor.

`DatabaseIntegrityTests` verifica restricciones críticas sobre una instalación limpia; no reemplaza MySQL ni el saneamiento histórico.
