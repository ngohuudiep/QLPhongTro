# Hướng dẫn chạy demo QLPhongTro trên Windows

## 1. Yêu cầu

- Windows 64-bit.
- JDK 21 tại `D:\Java\jdk-21.0.11`.
- Maven 3.9.6 trong `PATH`.
- SQL Server đang chạy trên `localhost:1433`, có database `ApartmentDB`.
- Tài khoản Windows hiện tại được cấp quyền cần thiết trên `ApartmentDB`.
- Mạng Internet ở lần chạy đầu để tải thư viện JDBC Windows Authentication chính thức của Microsoft.

Ứng dụng không tự tạo tài khoản hoặc dữ liệu demo. Dữ liệu hiện có được giữ nguyên. Không chạy `setup_db.ps1` để khởi tạo database trên dữ liệu đang sử dụng.

## 2. Kiểm tra Java và Maven

Mở CMD:

```bat
set "JAVA_HOME=D:\Java\jdk-21.0.11"
set "PATH=%JAVA_HOME%\bin;%PATH%"
java -version
mvn -version
```

Kết quả cần hiển thị Java 21.0.11 và Maven 3.9.6.

## 3. Kiểm tra SQL Server

Xác nhận dịch vụ SQL Server đang chạy và TCP/IP bật tại cổng 1433. Có thể kiểm tra kết nối Windows Authentication bằng:

```bat
sqlcmd -S localhost -E -d ApartmentDB -Q "SELECT DB_NAME()"
```

Nếu báo lỗi đăng nhập/quyền, hãy cấp quyền cho tài khoản Windows hiện tại trên database hoặc nhờ quản trị viên cấu hình SQL Server. Không cần gửi mật khẩu cho ứng dụng khi chạy profile Windows-auth.

## 4. Chạy ứng dụng

Cách khuyến nghị: mở CMD và chạy:

```bat
cd /d C:\Users\Admin\Downloads\QLPhongTro-main
run_app.bat
```

Launcher sẽ kiểm tra JDK, Maven, SQL Server và cổng 8080; cài thư viện Windows Authentication đã xác minh chữ ký Microsoft vào thư mục dữ liệu riêng của user; sau đó chạy Spring Boot bằng profile `windows-auth`.

Khi log có dòng `Started QlphongtroApplication`, truy cập:

- Đăng nhập: http://localhost:8080/login
- Trang chủ: http://localhost:8080/

Giữ cửa sổ CMD mở trong lúc demo. Nhấn `Ctrl+C` tại cửa sổ đó để dừng ứng dụng.

Nếu chạy Maven thủ công, cần cài thư viện trước và kích hoạt profile:

```bat
cd /d C:\Users\Admin\Downloads\QLPhongTro-main
set "JAVA_HOME=D:\Java\jdk-21.0.11"
set "PATH=%JAVA_HOME%\bin;%PATH%"
powershell -NoProfile -ExecutionPolicy Bypass -File install_sqlserver_auth.ps1
set "AUTH_DLL_DIR=%USERPROFILE%\qlphongtro-data\mssql-jdbc-auth\12.4.2\x64"
mvn spring-boot:run -Dspring-boot.run.profiles=windows-auth "-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=false -Djava.library.path=%AUTH_DLL_DIR%"
```

## 5. Tài khoản và dữ liệu demo

Ứng dụng hiện không seed tài khoản, không reset mật khẩu và không tự tạo hồ sơ/phòng/hóa đơn demo. Mật khẩu đã băm trong database không thể đọc lại để xác minh mật khẩu đăng nhập. Không sử dụng mật khẩu mẫu không có bằng chứng.

Để xem các username/role hiện có mà không đọc password hash, dùng SQL Server Management Studio hoặc `sqlcmd` với Windows Authentication. Truy vấn chỉ đọc:

```sql
SELECT u.username, r.role_name, u.enabled, u.account_status
FROM dbo.users AS u
JOIN dbo.roles AS r ON r.role_id = u.role_id
ORDER BY r.role_name, u.username;
```

Đăng nhập bằng username/mật khẩu hiện có do người quản trị cung cấp. Tài khoản chủ trọ mới chỉ hoạt động sau khi được Admin duyệt theo luồng ứng dụng.

## 6. Gợi ý trình bày

Sau khi xác minh thông tin đăng nhập thực tế, có thể trình bày:

1. Trang đăng nhập và trang phòng công khai.
2. Dashboard Admin, danh sách tài khoản và hàng chờ duyệt chủ trọ (nếu có hồ sơ chờ).
3. Dashboard chủ trọ, khu trọ/phòng/người thuê/hợp đồng.
4. Dashboard người thuê, hợp đồng và yêu cầu chấm dứt (nếu có dữ liệu liên kết).
5. Hóa đơn và thanh toán chỉ khi có dữ liệu thật trong database.

Không tạo thao tác ghi trên database thật chỉ để làm đầy màn demo.

## 7. Xử lý lỗi thường gặp

- **Thiếu JDK/Maven:** kiểm tra lại đường dẫn JDK và `PATH`; mở CMD mới sau khi cài Maven.
- **SQL Server không kết nối:** kiểm tra dịch vụ SQL Server, TCP/IP, port 1433 và quyền của tài khoản Windows bằng `sqlcmd -S localhost -E -d ApartmentDB`.
- **Không tải được thư viện native:** cần truy cập mạng tới GitHub release của Microsoft hoặc tải thư viện theo hướng dẫn quản trị máy; launcher xác minh SHA-256 và chữ ký Microsoft trước khi sử dụng.
- **`UnsatisfiedLinkError`:** xác nhận JDK và Windows đều 64-bit; xóa thư mục `C:\Users\<user>\qlphongtro-data\mssql-jdbc-auth\12.4.2` nếu thư viện hỏng rồi chạy lại launcher.
- **Cổng 8080 đang dùng:** launcher sẽ báo PID; đóng đúng ứng dụng đang dùng cổng đó rồi chạy lại, không tự động dừng tiến trình khác.
- **Tài khoản không đăng nhập được:** kiểm tra đúng username/password, trạng thái tài khoản và việc Admin đã duyệt chủ trọ hay chưa.
- **Lỗi cột/bảng hoặc quyền DDL:** xem log Spring Boot. Cấu hình JPA hiện tại là `ddl-auto=update`; không đổi sang `create`/`create-drop` và không chạy script khởi tạo DB hiện có trên database thật.

## 8. Bảo mật và tình trạng xác minh

- Profile `windows-auth` dùng tài khoản Windows hiện tại; profile SQL login mặc định không bị thay đổi.
- Khóa remember-me của profile demo tự sinh nếu không được cấu hình; cookie remember-me sẽ mất hiệu lực sau khi ứng dụng khởi động lại.
- Không có mật khẩu DB hoặc mật khẩu demo nào được lưu trong tài liệu/source.
- Việc đăng nhập bằng ba vai trò và nội dung dữ liệu demo chỉ được xem là xác minh sau khi người vận hành đăng nhập bằng thông tin xác thực thật.
