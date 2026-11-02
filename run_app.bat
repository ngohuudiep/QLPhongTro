@echo off
setlocal
chcp 65001 >nul
title Quan Ly Phong Tro - Khoi Chay Ung Dung

echo ==========================================================
echo       DANG KHOI CHAY HE THONG QUAN LY PHONG TRO
echo ==========================================================
echo.

set MAVEN_DIR=%USERPROFILE%\apache-maven-3.9.6
set MVN_EXEC=mvn
set "MAVEN_OPTS=-Dfile.encoding=UTF-8"

if exist "%MAVEN_DIR%\bin\mvn.cmd" (
    set "MVN_EXEC=%MAVEN_DIR%\bin\mvn.cmd"
)

cd /d "%~dp0"

echo [1/2] Dang kiem tra va bien dich du an...
call "%MVN_EXEC%" compile -DskipTests
if %ERRORLEVEL% neq 0 (
    echo.
    echo [!] Co loi xay ra trong qua trinh bien dich.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [2/2] Dang khoi dong ung dung Spring Boot tai http://localhost:8080...
echo.

start "" powershell -NoProfile -ExecutionPolicy Bypass -Command "$ready = $false; for ($i=0; $i -lt 45; $i++) { Start-Sleep -Seconds 2; try { $tcp = New-Object System.Net.Sockets.TcpClient('127.0.0.1', 8080); if ($tcp.Connected) { $tcp.Close(); $ready = $true; break; } } catch {} } if ($ready) { Start-Process 'http://localhost:8080' }"

call "%MVN_EXEC%" spring-boot:run
pause
