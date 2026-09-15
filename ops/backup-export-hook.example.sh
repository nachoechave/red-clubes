#!/usr/bin/env sh
set -eu

if [ "$#" -ne 1 ]; then
  printf '%s\n' 'Este hook recibe la ruta del backup verificado como unico argumento.' >&2
  exit 2
fi

backup_file=$1
export_file=$backup_file

if [ -n "${BACKUP_AGE_RECIPIENT:-}" ]; then
  command -v age >/dev/null 2>&1 || { printf '%s\n' 'Falta instalar age.' >&2; exit 1; }
  export_file="$backup_file.age"
  age --recipient "$BACKUP_AGE_RECIPIENT" --output "$export_file.tmp" "$backup_file"
  mv -- "$export_file.tmp" "$export_file"
fi

if [ -n "${BACKUP_COPY_DIR:-}" ]; then
  mkdir -p "$BACKUP_COPY_DIR"
  cp -p -- "$export_file" "$BACKUP_COPY_DIR/"
  [ ! -f "$backup_file.sha256" ] || cp -p -- "$backup_file.sha256" "$BACKUP_COPY_DIR/"
fi

if [ "$export_file" = "$backup_file" ] && [ -z "${BACKUP_COPY_DIR:-}" ]; then
  printf '%s\n' 'Configura BACKUP_AGE_RECIPIENT y/o BACKUP_COPY_DIR.' >&2
  exit 2
fi

printf '%s\n' "Exportacion de backup completada: $export_file"
