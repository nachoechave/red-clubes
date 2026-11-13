#!/usr/bin/env sh
# Hook del piloto: requiere destino YA montado fuera del servidor.
set -eu
umask 077
[ "$#" -eq 1 ] || { echo 'Uso: backup-export-age.sh backup.sql.gz' >&2; exit 2; }
backup_file=$1
: "${BACKUP_AGE_RECIPIENT:?Configurar destinatario PUBLICO age}"
: "${BACKUP_COPY_DIR:?Configurar montaje externo dedicado}"
retention=${BACKUP_RETENTION_DAYS:-14}
case "$retention" in ''|*[!0-9]*) echo 'Retencion invalida' >&2; exit 2;; esac
case "$BACKUP_COPY_DIR" in /*) ;; *) echo 'El montaje debe ser absoluto' >&2; exit 2;; esac
[ "$BACKUP_COPY_DIR" != / ] || exit 2
command -v age >/dev/null
command -v mountpoint >/dev/null
mountpoint -q "$BACKUP_COPY_DIR" || { echo 'Destino externo sin montar; no copiar al disco local.' >&2; exit 1; }
[ -w "$BACKUP_COPY_DIR" ] || { echo 'Destino externo sin permiso de escritura' >&2; exit 1; }
[ -f "$backup_file.sha256" ] || { echo 'Falta checksum original' >&2; exit 1; }
(cd "$(dirname "$backup_file")" && sha256sum -c "$(basename "$backup_file").sha256")
base=$(basename "$backup_file")
case "$base" in red-clubes-*.sql.gz) ;; *) echo 'Nombre de backup invalido' >&2; exit 2;; esac
encrypted="$backup_file.age"
temporary_remote="$BACKUP_COPY_DIR/.$base.age.$$.tmp"
cleanup() { rm -f -- "$encrypted.tmp" "$temporary_remote"; }
trap cleanup EXIT HUP INT TERM
age -r "$BACKUP_AGE_RECIPIENT" -o "$encrypted.tmp" "$backup_file"
mv -- "$encrypted.tmp" "$encrypted"
(cd "$(dirname "$encrypted")" && sha256sum "$base.age" > "$base.age.sha256")
cp -- "$encrypted" "$temporary_remote"
expected=$(cut -d ' ' -f 1 "$encrypted.sha256")
actual=$(sha256sum "$temporary_remote" | cut -d ' ' -f 1)
[ "$expected" = "$actual" ] || { echo 'Copia cifrada corrupta' >&2; exit 1; }
mv -- "$temporary_remote" "$BACKUP_COPY_DIR/$base.age"
cp -- "$encrypted.sha256" "$backup_file.sha256" "$BACKUP_COPY_DIR/"
(cd "$BACKUP_COPY_DIR" && sha256sum -c "$base.age.sha256")
# Prefijo exclusivo en montaje dedicado; retencion solo despues de copia verificada.
find "$BACKUP_COPY_DIR" -maxdepth 1 -type f \( -name 'red-clubes-*.sql.gz.age' -o -name 'red-clubes-*.sql.gz.age.sha256' -o -name 'red-clubes-*.sql.gz.sha256' \) -mtime "+$retention" -delete
echo 'Backup cifrado copiado y verificado en montaje externo.'
