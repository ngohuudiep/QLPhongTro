@echo off
chcp 65001 >nul
echo Dang chay script tao co so du lieu SQL Server...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup_db.ps1"
pause
