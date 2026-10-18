@echo off
chcp 65001 >nul
title Dat Lai Mat Khau SA Cho SQL Server

echo ==========================================================
echo       DANG DAT LAI MAT KHAU CHO TAI KHOAN SA (SQL SERVER)
echo ==========================================================
echo.
echo Mat khau mac dinh se duoc dat la: 123456
echo.

set /p NEW_PASS="Nhap mat khau moi (de trong neu muon dung 123456): "
if "%NEW_PASS%"=="" set NEW_PASS=123456

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0reset_sa_password.ps1" -NewPassword "%NEW_PASS%"
pause
