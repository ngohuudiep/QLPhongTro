@echo off
setlocal

set MAVEN_VERSION=3.9.6
set MAVEN_DIR=%USERPROFILE%\apache-maven-%MAVEN_VERSION%
set MVN_CMD=%MAVEN_DIR%\bin\mvn.cmd

if not exist "%MVN_CMD%" (
    echo Khong tim thay Maven, dang tu dong cai dat qua install_maven.bat...
    call "%~dp0install_maven.bat"
)

if exist "%MVN_CMD%" (
    call "%MVN_CMD%" %*
) else (
    echo Khong the chay Maven. Vui long chay install_maven.bat truoc!
    exit /b 1
)
