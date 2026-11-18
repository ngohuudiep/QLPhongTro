@echo off
setlocal EnableDelayedExpansion

if defined JDK17_HOME (
    call :validate "!JDK17_HOME!"
    if errorlevel 1 (
        echo JDK17_HOME does not point to a valid JDK 17 installation.
        goto :missing
    )
) else (
    for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-17*") do if not defined JDK17_HOME if exist "%%~fD\bin\java.exe" set "JDK17_HOME=%%~fD"
    for /d %%D in ("%ProgramFiles%\Java\jdk-17*") do if not defined JDK17_HOME if exist "%%~fD\bin\java.exe" set "JDK17_HOME=%%~fD"
    for /d %%D in ("%ProgramFiles%\Microsoft\jdk-17*") do if not defined JDK17_HOME if exist "%%~fD\bin\java.exe" set "JDK17_HOME=%%~fD"
    if not defined JDK17_HOME (
        echo JDK 17 was not found in the standard installation locations.
        goto :missing
    )
    call :validate "!JDK17_HOME!"
    if errorlevel 1 (
        echo The detected installation is not a valid JDK 17.
        goto :missing
    )
)

set "JAVA_HOME=%JDK17_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
java -version
if errorlevel 1 exit /b 1

call mvn %*
exit /b %ERRORLEVEL%

:validate
if not exist "%~1\bin\java.exe" exit /b 1
set "DETECTED_JAVA_VERSION="
for /f "tokens=3" %%V in ('"%~1\bin\java.exe" -version 2^>^&1') do if not defined DETECTED_JAVA_VERSION set "DETECTED_JAVA_VERSION=%%~V"
if not "!DETECTED_JAVA_VERSION:~0,3!"=="17." exit /b 1
exit /b 0

:missing
echo Install Eclipse Temurin JDK 17, then set JDK17_HOME to its installation directory.
echo Example: set "JDK17_HOME=C:\Program Files\Eclipse Adoptium\jdk-17"
exit /b 1
