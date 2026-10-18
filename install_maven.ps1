# Script tự động cài đặt Apache Maven và tải toàn bộ thư viện dự án QLPhongTro
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   BẮT ĐẦU CÀI ĐẶT APACHE MAVEN VÀ TẢI THƯ VIỆN DỰ ÁN    " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Kiểm tra Java
Write-Host "`n[1/4] Kiểm tra Java..." -ForegroundColor Yellow
try {
    $javaVer = & java -version 2>&1
    if ($LASTEXITCODE -eq 0 -or $javaVer) {
        Write-Host "-> Đã tìm thấy Java:" -ForegroundColor Green
        $javaVer | ForEach-Object { Write-Host "   $_" -ForegroundColor Gray }
    }
} catch {
    Write-Host "-> CẢNH BÁO: Chưa tìm thấy lệnh 'java'. Bạn cần cài đặt JDK 17 hoặc mới hơn để chạy ứng dụng!" -ForegroundColor Red
}

# 2. Chuẩn bị thư mục cài đặt Maven
$ProgressPreference = 'SilentlyContinue'
$mavenVersion = "3.9.6"
$installFolder = "$env:USERPROFILE\apache-maven-$mavenVersion"
$binFolder = "$installFolder\bin"
$zipPath = "$env:TEMP\apache-maven-$mavenVersion-bin.zip"
$mavenUrl = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"

if (-not (Test-Path "$binFolder\mvn.cmd")) {
    Write-Host "`n[2/4] Đang tải Apache Maven $mavenVersion từ Apache..." -ForegroundColor Yellow
    Write-Host "URL: $mavenUrl" -ForegroundColor Gray
    
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $webClient = New-Object System.Net.WebClient
    $webClient.DownloadFile($mavenUrl, $zipPath)
    
    Write-Host "-> Đang giải nén Maven vào: $installFolder" -ForegroundColor Yellow
    Expand-Archive -Path $zipPath -DestinationPath "$env:USERPROFILE" -Force
    Remove-Item -Path $zipPath -Force -ErrorAction SilentlyContinue
    Write-Host "-> Giải nén hoàn tất!" -ForegroundColor Green
} else {
    Write-Host "`n[2/4] Apache Maven đã có sẵn tại: $installFolder" -ForegroundColor Green
}

# 3. Cập nhật biến môi trường PATH
Write-Host "`n[3/4] Cập nhật biến môi trường PATH..." -ForegroundColor Yellow
$userPath = [Environment]::GetEnvironmentVariable("Path", [EnvironmentVariableTarget]::User)
if ($userPath -notlike "*$binFolder*") {
    [Environment]::SetEnvironmentVariable("Path", "$binFolder;$userPath", [EnvironmentVariableTarget]::User)
    Write-Host "-> Đã thêm $binFolder vào biến môi trường PATH vĩnh viễn!" -ForegroundColor Green
} else {
    Write-Host "-> Maven đã có trong PATH." -ForegroundColor Green
}

# Bổ sung vào session hiện tại
$env:Path = "$binFolder;$env:Path"

# 4. Tải toàn bộ dependencies cho dự án
Write-Host "`n[4/4] Đang tải tất cả thư viện dự án (Spring Boot, Jakarta, Lombok, Thymeleaf)..." -ForegroundColor Yellow
Set-Location -Path $PSScriptRoot

& "$binFolder\mvn.cmd" clean compile -DskipTests

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n==========================================================" -ForegroundColor Green
    Write-Host " THÀNH CÔNG: ĐÃ TẢI XONG TẤT CẢ THƯ VIỆN CHO DỰ ÁN!      " -ForegroundColor Green
    Write-Host " Tất cả lỗi import jakarta và lỗi đỏ sẽ tự động biến mất." -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Green
} else {
    Write-Host "`nQuá trình biên dịch gặp vấn đề, vui lòng kiểm tra kết nối mạng hoặc lỗi Java." -ForegroundColor Red
}
