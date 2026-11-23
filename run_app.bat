@echo off
setlocal
chcp 65001 >nul
title QLPhongTro - Demo

set "JAVA_HOME=C:\Program Files\Java\jdk-17"
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [LOI] Khong tim thay JDK tai "%JAVA_HOME%".
    pause
    exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "AUTH_DLL_DIR=%USERPROFILE%\qlphongtro-data\mssql-jdbc-auth\12.4.2\x64"

set "MAVEN_OPTS=-Dfile.encoding=UTF-8"

where mvn >nul 2>nul
if errorlevel 1 (
    echo [LOI] Khong tim thay Maven trong PATH.
    echo Cai Maven 3.9.6 hoac them thu muc bin cua Maven vao PATH.
    pause
    exit /b 1
)

cd /d "%~dp0"

echo Dang kiem tra SQL Server localhost:1435 (SQLEXPRESS01)...
powershell -NoProfile -Command "if (Test-NetConnection localhost -Port 1435 -InformationLevel Quiet) { exit 0 } else { exit 1 }"
if errorlevel 1 (
    echo [LOI] Khong ket noi duoc SQLEXPRESS01 tai localhost:1435.
    echo Hay bat TCP/IP port 1435 cho SQLEXPRESS01 va restart service MSSQL$SQLEXPRESS01.
    pause
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0install_sqlserver_auth.ps1"
if errorlevel 1 (
    echo [LOI] Khong the cai thu vien xac thuc Windows cho SQL Server.
    pause
    exit /b 1
)

for /f "delims=" %%P in ('powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty OwningProcess"') do set "PORT_PID=%%P"
if defined PORT_PID (
    echo [LOI] Cong 8080 dang duoc su dung boi PID %PORT_PID%.
    echo Hay dong ung dung dang dung cong nay roi chay lai.
    pause
    exit /b 1
)

echo Dang khoi dong QLPhongTro voi Windows Authentication...
echo Khi thay dong "Started QlphongtroApplication", mo http://localhost:8080/login
call mvn spring-boot:run -Dspring-boot.run.profiles=windows-auth "-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=false -Djava.library.path=%AUTH_DLL_DIR%"
set "APP_EXIT_CODE=%ERRORLEVEL%"
if not "%APP_EXIT_CODE%"=="0" (
    echo [LOI] Spring Boot da dung voi ma loi %APP_EXIT_CODE%.
)
pause
exit /b %APP_EXIT_CODE%
