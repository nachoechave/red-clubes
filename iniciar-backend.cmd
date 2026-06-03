@echo off
cd /d "%~dp0backend"

set "JAR=%CD%\target\backend-0.0.1-SNAPSHOT.jar"
set "LOG=%CD%\backend.log"

if not exist "%JAR%" (
  echo No se encontro el backend empaquetado:
  echo %JAR%
  echo Pedile a Codex que genere el paquete del backend.
  pause
  exit /b 1
)

echo Iniciando backend en http://127.0.0.1:8080
echo El detalle del arranque queda en: %LOG%
echo.
java -jar "%JAR%" 2>&1 | powershell -NoProfile -Command "$input | Tee-Object -FilePath '%LOG%'"
pause