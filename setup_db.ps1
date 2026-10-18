# ===================================================
# Script tu dong ket noi SQL Server va tao ApartmentDB
# ===================================================
# Thử kết nối lần lượt các instance thường gặp: .\SQLEXPRESS01, .\SQLEXPRESS, .\MAY1, localhost
$candidateServers = @(".\SQLEXPRESS01", ".\SQLEXPRESS", ".\MAY1", "localhost", ".")
$conn = $null

foreach ($srv in $candidateServers) {
    Write-Host ">>> Dang thu ket noi den SQL Server $srv..." -ForegroundColor Cyan
    try {
        $testConn = New-Object System.Data.SqlClient.SqlConnection("Server=$srv;Database=master;Integrated Security=True;TrustServerCertificate=True;Connect Timeout=3;")
        $testConn.Open()
        $conn = $testConn
        Write-Host ">>> Ket noi thanh cong den SQL Server ($srv)!" -ForegroundColor Green
        break
    } catch {
        # Thu instance tiep theo
    }
}

if (-not $conn) {
    Write-Host "[!] Khong the ket noi den bat ky SQL Server instance nao tren may!" -ForegroundColor Red
    Write-Host "Vui long kiem tra lai dich vu SQL Server (SQL Server Configuration Manager)." -ForegroundColor Yellow
    exit 1
}

try {

    $sqlPath = Join-Path $PSScriptRoot "ApartmentDB.sql"
    if (Test-Path $sqlPath) {
        Write-Host ">>> Dang chay file ApartmentDB.sql de tao Database va cac bang..." -ForegroundColor Cyan
        $sqlContent = Get-Content -Raw -Path $sqlPath -Encoding UTF8
        $batches = $sqlContent -split "(?im)^\s*GO\s*$"
        foreach ($batch in $batches) {
            $trimmed = $batch.Trim()
            if ($trimmed.Length -gt 0) {
                $cmd = $conn.CreateCommand()
                $cmd.CommandText = $trimmed
                $null = $cmd.ExecuteNonQuery()
            }
        }
        Write-Host ">>> Da tao Database ApartmentDB va cac bang thanh cong!" -ForegroundColor Green
    } else {
        Write-Host ">>> Khong tim thay file ApartmentDB.sql tai: $sqlPath" -ForegroundColor Red
        return
    }

    # Kiem tra cac bang da tao
    $conn.ChangeDatabase("ApartmentDB")
    $cmd = $conn.CreateCommand()
    $cmd.CommandText = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'"
    $reader = $cmd.ExecuteReader()
    Write-Host "`n>>> Danh sach cac bang da duoc tao trong ApartmentDB:" -ForegroundColor Yellow
    while ($reader.Read()) {
        Write-Host ("   [OK] Bang: " + $reader[0]) -ForegroundColor Green
    }
    $reader.Close()

    Write-Host "`n===============================================" -ForegroundColor Green
    Write-Host "HOAN TAT! Database ApartmentDB da san sang su dung!" -ForegroundColor Green
    Write-Host "===============================================" -ForegroundColor Green
} catch {
    Write-Host "`n[!] Co loi xay ra khi ket noi:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
} finally {
    if ($conn.State -eq [System.Data.ConnectionState]::Open) {
        $conn.Close()
    }
}

Write-Host "`nNhan phim bat ky de thoat..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
