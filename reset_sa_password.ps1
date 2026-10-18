# ==========================================================
# Script dat lai mat khau cho tai khoan sa trong SQL Server
# ==========================================================
param (
    [string]$NewPassword = "123456"
)

$ErrorActionPreference = "Stop"

# Kiem tra quyen Admin
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host ">>> Dang yeu cau quyen Administrator..." -ForegroundColor Yellow
    try {
        Start-Process powershell.exe -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`" -NewPassword `"$NewPassword`"" -Verb RunAs
        exit
    } catch {
        Write-Host "[!] Can chay voi quyen Run as Administrator de doi mat khau sa!" -ForegroundColor Red
        pause
        exit 1
    }
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "      DANG DAT LAI MAT KHAU TAI KHOAN SA SQL SERVER       " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ">>> Mat khau moi se duoc dat la: $NewPassword" -ForegroundColor Yellow

# 1. Bat Mixed Authentication Mode (LoginMode = 2) trong Registry
$instanceMapPath = "HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\Instance Names\SQL"
if (Test-Path $instanceMapPath) {
    $allProps = Get-ItemProperty -Path $instanceMapPath
    $instances = $allProps.PSObject.Properties | Where-Object { $_.Name -notmatch "^_|^PS" }
    foreach ($inst in $instances) {
        $instKey = $inst.Value
        $secPath = "HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\$instKey\MSSQLServer"
        if (Test-Path $secPath) {
            Set-ItemProperty -Path $secPath -Name "LoginMode" -Value 2
            Write-Host ">>> [OK] Da bat che do Mixed Mode (SQL + Windows Auth) cho $instKey" -ForegroundColor Green
        }
    }
}

# 2. Ket noi bang Windows Authentication va doi mat khau sa
$candidateServers = @(".\SQLEXPRESS01", "LAPTOP-12OGD3V1\SQLEXPRESS01", ".\SQLEXPRESS", "LAPTOP-12OGD3V1\SQLEXPRESS", ".\MAY1", "localhost", ".")
$successCount = 0

foreach ($srv in $candidateServers) {
    Write-Host ">>> Dang thu ket noi den SQL Server $srv bang Windows Authentication..." -ForegroundColor Cyan
    try {
        $conn = New-Object System.Data.SqlClient.SqlConnection("Server=$srv;Database=master;Integrated Security=True;TrustServerCertificate=True;Connect Timeout=3;")
        $conn.Open()
        
        $sql = @"
ALTER LOGIN [sa] WITH PASSWORD = '$NewPassword', CHECK_POLICY = OFF;
ALTER LOGIN [sa] ENABLE;
"@
        $cmd = $conn.CreateCommand()
        $cmd.CommandText = $sql
        $null = $cmd.ExecuteNonQuery()
        $conn.Close()
        
        Write-Host ">>> [THANH CONG] Da doi mat khau sa thanh: $NewPassword tren $srv!" -ForegroundColor Green
        $successCount++
    } catch {
        # Bo qua neu instance khong chay
    }
}

if ($successCount -eq 0) {
    Write-Host "`n[!] Khong the ket noi bang Windows Authentication de doi mat khau sa." -ForegroundColor Red
    Write-Host "Vui long kiem tra lai SQL Server dang chay hoac doi qua SSMS." -ForegroundColor Yellow
    pause
    exit 1
}

# 3. Khoi dong lai dich vu SQL Server de nhan LoginMode moi
Write-Host ">>> Dang khoi dong lai dich vu SQL Server..." -ForegroundColor Cyan
$sqlServices = Get-Service | Where-Object { $_.Name -like "MSSQL*" }
foreach ($s in $sqlServices) {
    try {
        Restart-Service -Name $s.Name -Force -ErrorAction SilentlyContinue
        Write-Host ">>> [OK] Da khoi dong lai dich vu $($s.Name)" -ForegroundColor Green
    } catch {
        # ignore
    }
}

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host " HOAN TAT! Tai khoan sa da san sang:" -ForegroundColor Green
Write-Host "   - Username: sa" -ForegroundColor Yellow
Write-Host "   - Password: $NewPassword" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "Nhan phim bat ky de tiep tuc..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
