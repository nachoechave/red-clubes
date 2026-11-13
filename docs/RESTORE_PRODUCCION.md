# Recuperación y restore productivo

Runbook manual. Requiere responsable, ventana aprobada, backup elegido, impacto/RPO aceptado y versión compatible. **Nunca invocar automáticamente el restore desde deploy, health ni timer.**

## 1. Contener y conservar

1. Registrar incidente, hora UTC, release/SHA e image IDs actuales, última escritura aceptada y responsable.
2. Bloquear tráfico del virtual host (página de mantenimiento) y detener frontend/backend: `docker compose stop frontend backend`. Pausar timer: `sudo systemctl stop red-clubes-backup.timer`; esperar que un backup activo termine.
3. Conservar volumen original. Si está accesible, crear backup de incidente con exportación exitosa; no borrar datos para liberar espacio. Registrar conteos.
4. Seleccionar backup anterior al incidente y release compatible; comprobar espacio para descarga, descifrado y SQL. Descargar del almacenamiento externo a directorio privado de recuperación **fuera de BACKUP_DIR y su retención**.

## 2. Verificar y descifrar

En estación segura con identidad age:
```bash
umask 077
cd /ruta/privada/recuperacion
# Reemplazar nombre por el registrado. No introducir claves en argumentos.
sha256sum -c red-clubes-FECHA.sql.gz.age.sha256
age -d -i /ruta/segura/identidad-red-clubes.txt -o red-clubes-FECHA.sql.gz red-clubes-FECHA.sql.gz.age
sha256sum -c red-clubes-FECHA.sql.gz.sha256
gzip -t red-clubes-FECHA.sql.gz
```
Transferir plaintext y checksum por SSH al directorio privado del servidor de recuperación (0700/0600); si se descifra allí excepcionalmente, la clave no debe quedar en ese host. Verificar nuevamente SHA. Checksum comprueba integridad accidental, no autenticidad de un atacante que cambió ambos archivos: usar fuente/custodia verificadas.

Ensayar primero `sh ops/restore-test.sh /ruta/privada/recuperacion/red-clubes-FECHA.sql.gz` (MySQL temporal, sin red/puertos, eliminado al terminar). Verifica tablas y Flyway, **no todas las filas ni reglas de negocio**. Para aceptación restaurar también una copia de ensayo completa y comparar conteos/totales/circuitos.

## 3. Elegir destino correcto

`restore-mysql.sh` importa el dump en MYSQL_DATABASE y detiene backend. El dump reemplaza tablas incluidas; **no elimina tablas extra de versiones posteriores**. No es un rollback de esquema completo si hubo migración destructiva/incompatible.

- Misma versión/esquema: tras aprobar impacto, puede usarse el script sobre el destino verificado.
- Esquema incompatible: preferir servidor/instalación de recuperación separados, MySQL vacío y volumen NUEVO; preservar el anterior. Elegir una base nueva y verificar credenciales/permisos, arrancar solo MySQL, importar antes de arrancar backend del release compatible. No apuntar a una base con tablas sobrantes.
- En un destino realmente vacío, el backup previo automático no contiene CREATE TABLE y fallará. Solo allí, tras comprobar tabla_count=0 y conservar backup del origen, el operador puede añadir `SKIP_PRE_RESTORE_BACKUP=true`. No usar esta excepción en una instalación existente.

## 4. Restaurar con aprobación explícita

Cargar BACKUP_* como indica BACKUP_PILOTO para que el backup previo también se exporte. Verificar `docker compose ps`, proyecto y nombres de base (sin imprimir secretos). El script exige checksum, gzip válido y sentencias CREATE TABLE antes de detener backend:

```bash
CONFIRM_RESTORE=RESTAURAR ALLOW_PRODUCTION_RESTORE=true \
  sh ops/restore-mysql.sh /ruta/privada/recuperacion/red-clubes-FECHA.sql.gz
```

Si falla, mantener mantenimiento/backend detenido. No activar tráfico ni reintentar a ciegas. En DB vacía de recuperación exclusivamente usar la excepción documentada. Nunca editar V1–V6, checksums ni ejecutar repair para forzar arranque.

## 5. Volver al servicio

1. Fijar tag Git e IMAGE_TAG **compatibles con historial restaurado**; conservar imágenes previas, `docker load` si hace falta. Registrar DB_HOST/nombre/volumen destino.
2. Confirmar bootstrap/demo apagados, prod/validate activos y baseline=false.
3. `docker compose up -d --no-build --pull never --wait --wait-timeout 240`.
4. Comprobar logs Flyway, filas success y versión final esperada; si hay migraciones posteriores, requieren aprobación antes de arrancar.
5. `BASE_URL=http://127.0.0.1:8080 sh ops/smoke-test.sh`, luego HTTPS externo con REQUIRE_HTTPS=true. Adaptar APP_PORT.
6. Comprobar conteos exactos de socios, actividades, inscripciones, cuotas y pagos por club; totales de importes del corte; login, logout, cambio password y aislamiento A/B. Las sesiones del backup podrían restaurar sesiones anteriores: evaluar revocación operativa antes de reabrir.
7. Abrir tráfico solo con aceptación. Reanudar timer, ejecutar un backup nuevo completo y probar copia externa.
8. Registrar archivo/checksums, release/digests, inicio/fin/RTO medido, RPO/pérdida aceptada, pruebas y responsable. Eliminar plaintext temporales según política aprobada; no eliminar evidencia original hasta cierre del incidente.

## Rollback de aplicación sin restaurar DB

Si las migraciones son compatibles: parar tráfico, volver al checkout e IMAGE_TAG anterior, reutilizar imágenes conservadas con `--no-build --pull never`, esperar health y smoke. No ejecutar downgrade Flyway. Si la compatibilidad no está demostrada, usar la recuperación completa anterior.
