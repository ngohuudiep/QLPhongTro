@echo off
chcp 65001 >nul
title Cai Dat Maven va Thu Vien Spring Boot

echo ==========================================================
echo    DANG CAI DAT APACHE MAVEN VA TAI THU VIEN DU AN
echo ==========================================================

set MAVEN_VER=3.9.6
set MAVEN_DIR=%USERPROFILE%\apache-maven-%MAVEN_VER%
set MVN_EXEC=%MAVEN_DIR%\bin\mvn.cmd

if exist "%MVN_EXEC%" goto :RUN_MAVEN

echo [1/3] Dang tai Apache Maven %MAVEN_VER% (chi khoang 9MB)...
curl -L -o "%TEMP%\maven.zip" "https://archive.apache.org/dist/maven/maven-3/%MAVEN_VER%/binaries/apache-maven-%MAVEN_VER%-bin.zip"

echo [2/3] Dang giai nen Maven vao may...
tar -xf "%TEMP%\maven.zip" -C "%USERPROFILE%"
del "%TEMP%\maven.zip" >nul 2>&1

:: Them vao PATH vinh vien cho User
for /f "tokens=2*" %%a in ('reg query HKCU\Environment /v Path 2^>nul') do set "USER_PATH=%%b"
if not defined USER_PATH (
    setx Path "%MAVEN_DIR%\bin" >nul
) else (
    echo %USER_PATH% | find /i "%MAVEN_DIR%\bin" >nul || setx Path "%MAVEN_DIR%\bin;%USER_PATH%" >nul
)
set "Path=%MAVEN_DIR%\bin;%Path%"

:RUN_MAVEN
echo.
echo [3/3] Dang tai toan bo thu vien tu pom.xml ve may...
echo (Dang tai Spring Boot, Jakarta Persistence, Hibernate, Lombok...)
cd /d "%~dp0"
call "%MVN_EXEC%" clean compile -DskipTests

if %ERRORLEVEL% equ 0 (
    echo.
    echo ==========================================================
    echo  THANH CONG: DA TAI XONG TOAN BO THU VIEN CHO DU AN!
    echo  Loi "import jakarta cannot be resolved" da duoc giai quyet!
    echo ==========================================================
) else (
    echo.
    echo Co loi xay ra trong qua trinh tai thu vien.
)

pause
