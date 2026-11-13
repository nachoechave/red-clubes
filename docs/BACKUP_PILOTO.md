# Backup diario: local + cifrado fuera del servidor

Se reutiliza `ops/backup-mysql.sh`: dump transaccional de MySQL, gzip, SHA-256, publicación atómica y retención. No es copia de volúmenes ni incluye imágenes/configuración del host. No ejecutar cambios DDL durante dump. Copiar aparte release, configuración sin secretos y secretos mediante canal privado.

## Destinatario y destino

En una estación administrativa segura (fuera del Cloud Server), generar la identidad age: `age-keygen -o identidad-red-clubes.txt`. Guardarla 0600 y una copia offline custodiada. **Solo el destinatario público age1... llega al servidor**. No guardar claves privadas en repo, .env, imagen o backup del mismo servidor. Ensayar descifrado antes del piloto.

Preparar un montaje remoto dedicado `/mnt/red-clubes-offsite` (NAS/almacenamiento independiente, transporte cifrado, permisos para redclubes). Documentar proveedor, montaje persistente y su política de retención. `findmnt -T /mnt/red-clubes-offsite` debe mostrar el remoto esperado. El hook exige mountpoint: detecta montaje ausente, pero **no demuestra que un disco montado sea remoto**. El operador debe comprobar ubicación real desde otra máquina.

`backup-export-age.sh` cifra, calcula checksum ciphertext, copia mediante temporal, verifica lectura remota y conserva también checksum plaintext para verificar después del descifrado. Copia solo cifrado + checksums. Retención: 14 días tanto local como remoto, basada en mtime (find -mtime +14 borra después de más de 14 períodos completos de 24 h). Mantener una copia mensual protegida fuera de este prefijo antes de ensayos de recuperación, si la política lo exige. No usar el montaje para otras aplicaciones.

## Timer systemd

Instalar, adaptando usuario/rutas/montaje si difieren:
```bash
sudo install -o root -g redclubes -m 0640 ops/systemd/backup.env.example /etc/red-clubes/backup.env
# Editar recipient publico, montaje y rutas (sin passwords DB).
sudo install -o root -g root -m 0644 ops/systemd/red-clubes-backup.service ops/systemd/red-clubes-backup.timer ops/systemd/red-clubes-backup-failure.service /etc/systemd/system/
sudo systemd-analyze verify /etc/systemd/system/red-clubes-backup.service /etc/systemd/system/red-clubes-backup.timer
sudo systemctl daemon-reload
sudo systemctl start red-clubes-backup.service
sudo systemctl status red-clubes-backup.service
sudo journalctl -u red-clubes-backup.service --since today --no-pager
sudo systemctl enable --now red-clubes-backup.timer
systemctl list-timers red-clubes-backup.timer
```

Completar backup.env antes de start; no dejar REPLACE_WITH. `RequiresMountsFor` debe coincidir con el montaje adaptado. OnCalendar usa hora del servidor; documentar zona/UTC, ver `timedatectl`. Persistent ejecuta un backup atrasado cuando vuelve el host. Servicio no se solapa consigo mismo; evitar ejecuciones manuales paralelas. Timeout 30 min: medir y adaptar si el dump crece.

El servicio lee BACKUP_* desde EnvironmentFile; Docker Compose lee .env desde /opt/red-clubes. **No basta con escribir BACKUP_* en .env.** Para operación manual como redclubes, solo sobre archivo confiable root-owned:
```bash
set -a
. /etc/red-clubes/backup.env
set +a
sh ops/backup-mysql.sh
```
Usar sintaxis simple KEY=value, valores sin expansión shell. No sourcear archivos recibidos de terceros.

## Verificación y fallos

```bash
cd /var/backups/red-clubes
name=$(cat .last-success)
sha256sum -c "$name.sha256"
gzip -t "$name"
cd /mnt/red-clubes-offsite
sha256sum -c "$name.age.sha256"
```

`.last-success` solo avanza después del hook exitoso. Si cifrado/montaje/copia falla, el servicio falla, conserva el último marcador y el dump local recién creado; revisar error y repetir sin ocultarlo. Una copia antigua del marcador previa a este cambio no demuestra exportación. Configurar destinatario humano y monitor del proveedor para failed unit/backup >26 h; failure.service solo registra en journal, **no envía avisos**.

Desde otro equipo descargar `.sql.gz.age`, su `.age.sha256` y el `.sql.gz.sha256`; validar ciphertext, descifrar allí, verificar plaintext, `gzip -t`, ejecutar restore aislado. Este recorrido verifica que la pérdida total del host es recuperable. El CI prueba dump/restore locales, no ese almacenamiento ni la clave custodiada.

RPO inicial: hasta 24 horas desde último backup correcto (más si hubo fallos); RTO pendiente de medir. Ensayo aislado mensual y antes de migraciones; registrar duración, filas por tabla y resultado funcional. Ver [restore](RESTORE_PRODUCCION.md).
