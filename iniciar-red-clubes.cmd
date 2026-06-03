@echo off
cd /d "%~dp0"
start "Red Clubes Backend" cmd /k "%~dp0iniciar-backend.cmd"
timeout /t 15 /nobreak > nul
start "Red Clubes Frontend" cmd /k "%~dp0iniciar-frontend.cmd"
