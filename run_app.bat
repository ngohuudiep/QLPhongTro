@echo off
chcp 65001 >nul
title Quan Ly Phong Tro - Khoi Chay Ung Dung

echo ==========================================================
echo       DANG KHOI CHAY HE THONG QUAN LY PHONG TRO
echo ==========================================================
echo.

set MAVEN_DIR=%USERPROFILE%\apache-maven-3.9.6
set MVN_EXEC=mvn

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
echo (Trinh duyet se tu dong mo trang chu sau khoang 10 giay khi ung dung san sang...)
echo.

:: Mo trinh duyet o tien trinh ngam sau 10 giay de cho Spring Boot khoi dong
start "" cmd /c "timeout /t 10 /nobreak >nul & start http://localhost:8080"

call "%MVN_EXEC%" spring-boot:run
pause
