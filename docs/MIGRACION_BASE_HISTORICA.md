# Adopción de una base histórica — procedimiento separado

No es el flujo del piloto limpio. Nunca ensayar sobre producción ni habilitar baseline indiscriminadamente. Requiere dueño de datos, operador y aprobación explícita antes de cambios.

1. **Backup obligatorio**: dump consistente, checksum, copia cifrada externa y evidencia de restore. Congelar escrituras/DDL cuando corresponda. Conservar imágenes/configuración anteriores. Si la base aún no tiene Flyway, `ops/restore-test.sh` fallará por ausencia de historial: ensayar manualmente en MySQL aislado sin puertos, importar y verificar tablas/conteos; no inventar historial para pasar el test.
2. **Clonar DB**: entorno separado, sin integraciones ni datos expuestos. Usar una copia anonimizada si hay datos reales y se comparte con desarrollo. No apuntar DB_URL de pruebas al origen.
3. **Inventariar** versión MySQL, collation, charset, sql_mode, zona horaria, tablas/columnas/tipos/nullability, tamaños y conteos exactos. Consultas de estructura:
   ```sql
   SELECT table_name,engine,table_collation FROM information_schema.tables WHERE table_schema=DATABASE();
   SELECT table_name,column_name,column_type,is_nullable FROM information_schema.columns WHERE table_schema=DATABASE() ORDER BY table_name,ordinal_position;
   SELECT table_name,constraint_name,constraint_type FROM information_schema.table_constraints WHERE table_schema=DATABASE();
   SELECT table_name,index_name,non_unique,seq_in_index,column_name FROM information_schema.statistics WHERE table_schema=DATABASE() ORDER BY table_name,index_name,seq_in_index;
   ```
4. **Comparar constraints/índices** con V1–V6 y un esquema nuevo de referencia. SHOW CREATE TABLE de cada tabla. Detectar duplicados por club+DNI, club+número socio, club+socio+actividad, club+socio+período y club+actividad+socio+fecha; revisar huérfanos y asignaciones fuera del club. No usar estimaciones information_schema.table_rows como conteos exactos.
5. **Usuarios históricos**: listar solo id/rol/estado de superusuarios y revisar cuentas demo antiguas contra inventario autorizado; no exportar password_hash ni tokens. El antiguo DNI demo no se borra solo al actualizar código. Desactivar/rotar esas cuentas y revocar sesiones mediante procedimiento aprobado, conservando un administrador válido. No borrarlas automáticamente ni romper foreign keys de auditoría/pagos.
6. **Compatibilidad Flyway**: si existe historial, comparar scripts/checksums/success con la release y detenerse ante divergencias. Si no existe, baseline=1 solo si el esquema equivale realmente a V1; baseline no crea constraints ni valida equivalencia. Si la DB ya tiene cambios parciales V2–V6, preparar reconciliación revisada, no ejecutar baseline=1 a ciegas.
7. **Ensayo baseline**: Compose fuerza FLYWAY_BASELINE_ON_MIGRATE=false. Para una copia que calificó, usar override temporal NO versionado de environment backend (`FLYWAY_BASELINE_ON_MIGRATE: "true"`), ejecutar con `-f docker-compose.yml -f /ruta/privada/baseline-copia.yml`. Eliminar override de ejecución tras el arranque y recrear con false. No alterar V1–V6 ni checksums. La versión por defecto es 1; cualquier baseline distinto requiere revisión especializada.
8. **Migrar copia**: Flyway V2→última cuando baseline=1, luego Hibernate validate. V3 transforma cuotas/pagos históricos; V4 revoca sesiones; V6 no adivina relación de profesores por nombre. Resolver valores no convertibles con dueño funcional, registrar decisiones.
9. **Pruebas**: conteos/totales antes/después, historial success, constraints reales, roles y aislamiento A/B, login legacy y rehash, smoke, backup y restore aislado del resultado ya migrado. Medir locks/duración/espacio.
10. **Plan de corte**: ventana aprobada, bloqueo de escrituras, backup final, repetir pasos ensayados exactamente y abortar si difieren. No automatizar SQL destructivo.
11. **Rollback**: mantener origen/backup anterior. Si DB dejó de ser compatible, recuperar backup en base vacía y usar binario anterior según RESTORE_PRODUCCION.md; no deshacer migraciones con SQL improvisado. Registrar RPO/RTO y aprobación de reapertura.

Hasta que el ensayo sea aceptado, **base histórica bloqueada** aunque CI de base nueva esté verde.
