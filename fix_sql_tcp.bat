@echo off
chcp 65001 >nul
title Bat TCP/IP cho SQL Server

:: Kiem tra quyen Admin va tu dong xin quyen neu chua co
net session >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo Dang yeu cau quyen Administrator...
    powershell -Command "Start-Process cmd -ArgumentList '/c \"\"%~f0\"\"' -Verb RunAs"
    exit /b
)

echo ==========================================================
echo        DANG BAT GIAO THUC TCP/IP CHO SQL SERVER
echo ==========================================================
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0fix_sql_tcp.ps1"
pause
