# ==========================================================
# Script tự động bật TCP/IP cho SQL Server instance MAY1
# ==========================================================
$ErrorActionPreference = "Stop"

# Tự động yêu cầu quyền Administrator nếu chưa có
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host ">>> Dang yeu cau quyen Administrator tu Windows..." -ForegroundColor Yellow
    try {
        Start-Process powershell.exe -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`"" -Verb RunAs
        exit
    } catch {
        Write-Host "[!] Khong the lay quyen Administrator: $_" -ForegroundColor Red
        Write-Host "Vui long mo PowerShell bang cach nhap chuot phai -> Run as Administrator!" -ForegroundColor Yellow
        $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
        exit 1
    }
}

$instanceName = "SQLEXPRESS01"
Write-Host ">>> Dang tim cau hinh cho SQL Server instance: $instanceName..." -ForegroundColor Cyan

# 1. Tìm Registry Key của instance
$instanceMapPath = "HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\Instance Names\SQL"
if (-not (Test-Path $instanceMapPath)) {
    Write-Host "[!] Khong tim thay SQL Server installed tren may tinh!" -ForegroundColor Red
    exit 1
}

$instanceKey = (Get-ItemProperty -Path $instanceMapPath -Name $instanceName -ErrorAction SilentlyContinue).$instanceName
if (-not $instanceKey) {
    # Nếu không tìm thấy tên MAY1, lấy instance đầu tiên tìm được
    $allProps = Get-ItemProperty -Path $instanceMapPath
    $propNames = $allProps.PSObject.Properties | Where-Object { $_.Name -notmatch "^_|^PS" } | Select-Object -ExpandProperty Name
    if ($propNames) {
        $firstInstance = $propNames[0]
        $instanceKey = $allProps.$firstInstance
        $instanceName = $firstInstance
        Write-Host ">>> Da tim thay instance: $instanceName ($instanceKey)" -ForegroundColor Yellow
    } else {
        Write-Host "[!] Khong tim thay bat ky SQL Server instance nao!" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host ">>> Da tim thay instance: $instanceName ($instanceKey)" -ForegroundColor Green
}

# 2. Bật TCP/IP trong Registry
$tcpPath = "HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\$instanceKey\MSSQLServer\SuperSocketNetLib\Tcp"
if (Test-Path $tcpPath) {
    Set-ItemProperty -Path $tcpPath -Name "Enabled" -Value 1
    Write-Host ">>> [OK] Da bat giao thuc TCP/IP (Enabled = 1)" -ForegroundColor Green

    # Cấu hình IPAll port 1433
    $ipAllPath = "$tcpPath\IPAll"
    if (Test-Path $ipAllPath) {
        Set-ItemProperty -Path $ipAllPath -Name "TcpPort" -Value "1433"
        Set-ItemProperty -Path $ipAllPath -Name "TcpDynamicPorts" -Value ""
        Write-Host ">>> [OK] Da gan cong co dinh TCP Port = 1433 vao IPAll" -ForegroundColor Green
    }
}

# 3. Kích hoạt và bật SQL Server Browser Service
try {
    Set-Service -Name "SQLBrowser" -StartupType Automatic -ErrorAction SilentlyContinue
    Start-Service -Name "SQLBrowser" -ErrorAction SilentlyContinue
    Write-Host ">>> [OK] Da khoi dong SQL Server Browser Service" -ForegroundColor Green
} catch {
    Write-Host ">>> [Chu y] SQLBrowser khong khoi dong duoc hoac khong can thiet." -ForegroundColor Yellow
}

# 4. Khởi động lại dịch vụ SQL Server
$sqlServiceName = if ($instanceName -eq "MSSQLSERVER") { "MSSQLSERVER" } else { "MSSQL`$$instanceName" }
Write-Host ">>> Dang khoi dong lai dich vu SQL Server ($sqlServiceName)..." -ForegroundColor Cyan
try {
    Restart-Service -Name $sqlServiceName -Force
    Write-Host ">>> [THANH CONG] SQL Server ($sqlServiceName) da duoc khoi dong lai voi TCP/IP!" -ForegroundColor Green
} catch {
    Write-Host "[!] Khong the khoi dong lai dich vu ${sqlServiceName}: $_" -ForegroundColor Red
}

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host " HOAN TAT! TCP/IP da duoc bat cho SQL Server instance $instanceName." -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "Nhan phim bat ky de tiep tuc..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
