# Script khôi phục dữ liệu demo ApartmentDB_v2
$scriptPath = Join-Path $PSScriptRoot "reset_demo_data.sql"
if (Test-Path $scriptPath) {
    Write-Host "Dang khoi phuc du lieu demo ApartmentDB_v2..." -ForegroundColor Cyan
    sqlcmd -S localhost,1435 -d ApartmentDB_v2 -E -i $scriptPath
} else {
    Write-Error "Khong tim thay file reset_demo_data.sql tai $scriptPath"
}
