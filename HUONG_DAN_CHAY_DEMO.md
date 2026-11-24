# Hướng dẫn chạy demo QLPhongTro trên Windows

## 1. Yêu cầu môi trường

- **Hệ điều hành:** Windows 64-bit.
- **Java:** JDK 17 cài đặt tại `C:\Program Files\Java\jdk-17`.
- **Maven:** Apache Maven 3.9.6 (hoặc sử dụng wrapper `scripts\mvn17.cmd`).
- **Node.js:** Node.js v18+ hoặc v20+ (để chạy bộ script kiểm chứng live-smoke).
- **Cơ sở dữ liệu:** SQL Server (Instance `LAPTOP-12OGD3V1\SQLEXPRESS01`), port **1435**, Database **`ApartmentDB_v2`** (Collation: `Vietnamese_CI_AS`).
- **Thư viện Windows Auth:** Microsoft mssql-jdbc_auth x64 (được script `install_sqlserver_auth.ps1` tự động cài đặt).

---

## 2. Tài khoản Demo có sẵn

Hệ thống đã được seed sẵn 3 tài khoản mẫu tương ứng với 3 vai trò (mật khẩu BCrypt chuẩn):

| Vai trò | Username | Mật khẩu mặc định | Trang chuyển hướng sau đăng nhập |
|---|---|---|---|
| **Quản trị viên (Admin)** | `admin` | `Admin@123456` | `/admin/dashboard` |
| **Chủ trọ (Landlord)** | `chutro` | `Landlord@123456` | `/landlord/dashboard` |
| **Khách thuê (Tenant)** | `nguoithue` | `Tenant@123456` | `/tenant/dashboard` |

> *Ghi chú:* Khi người dùng đăng ký tài khoản chủ trọ mới từ trang `/register`, tài khoản sẽ ở trạng thái chờ duyệt (`PENDING_APPROVAL`) và cần tài khoản `admin` đăng nhập vào `/admin/landlord-registrations` để phê duyệt thì chủ trọ mới có thể đăng nhập.

---

## 3. Khởi động ứng dụng

Mở PowerShell hoặc Command Prompt tại thư mục dự án và chạy:

```cmd
.\run_app.bat
```

Launcher `run_app.bat` sẽ tự động:
1. Thiết lập `JAVA_HOME=C:\Program Files\Java\jdk-17`.
2. Kiểm tra kết nối TCP/IP tới SQL Server tại `localhost:1435`.
3. Tải/kiểm tra thư viện Microsoft JDBC Auth DLL (`mssql-jdbc_auth-12.4.2.x64.dll`).
4. Kiểm tra cổng `8080` (tránh xung đột tiến trình).
5. Khởi động Spring Boot với profile `windows-auth`.
6. Flyway tự động kiểm tra và áp dụng các migration từ `V1` đến `V2_16`.

Khi xuất hiện dòng thông báo:
```text
Started QlphongtroApplication in ... seconds
```
Mở trình duyệt truy cập:
- **Trang chủ / Danh sách phòng:** [http://localhost:8080/](http://localhost:8080/)
- **Đăng nhập:** [http://localhost:8080/login](http://localhost:8080/login)
- **Đánh giá phòng (tự điều hướng theo vai trò):** [http://localhost:8080/reviews](http://localhost:8080/reviews)
- **Bảng xếp hạng chủ trọ & phòng:** [http://localhost:8080/rankings](http://localhost:8080/rankings)

---

## 4. Reset dữ liệu demo về trạng thái ban đầu

Sau khi chạy thử các luồng nghiệp vụ hoặc test tự động, có thể khôi phục dữ liệu demo sạch sẽ (14 phòng với trạng thái chuẩn, khôi phục tài khoản demo, dọn dẹp hợp đồng/hóa đơn test) bằng PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\reset_demo_data.ps1
```


---

## 4. Các lệnh kiểm chứng tự động (Live Verification)

### 4.1. Kiểm chứng đăng nhập 3 vai trò (Live-Smoke Test)
Chạy khi ứng dụng đang mở tại port 8080:

```powershell
node runtime-tests/live-smoke/admin.mjs
node runtime-tests/live-smoke/landlord.mjs
node runtime-tests/live-smoke/tenant.mjs
```

### 4.2. Kiểm chứng trọn vẹn 4 Luồng nghiệp vụ End-to-End
Chạy script kiểm chứng toàn diện tất cả các tính năng kinh doanh:

```powershell
node runtime-tests/verify-all-business-flows.mjs
```

Luồng kiểm chứng tự động bao gồm:
- **Luồng A:** Khách gửi yêu cầu thuê phòng $\rightarrow$ Chủ trọ duyệt & tạo nháp hợp đồng $\rightarrow$ Gửi hợp đồng $\rightarrow$ Khách ký chấp nhận $\rightarrow$ Khách gửi yêu cầu cọc $\rightarrow$ Chủ trọ duyệt cọc $\rightarrow$ Hợp đồng kích hoạt `ACTIVE`.
- **Luồng B:** Chủ trọ chốt chỉ số điện nước $\rightarrow$ Lập hóa đơn dịch vụ $\rightarrow$ Xác nhận thanh toán $\rightarrow$ Hóa đơn chuyển trạng thái `PAID`.
- **Luồng D:** Khách thuê gửi báo cáo sự cố kèm mô tả $\rightarrow$ Người dùng đăng ký chủ trọ mới $\rightarrow$ Admin duyệt hồ sơ chủ trọ $\rightarrow$ Admin khóa và mở khóa tài khoản người thuê.
- **Luồng C:** Khách gửi yêu cầu trả phòng $\rightarrow$ Chủ trọ duyệt $\rightarrow$ Chủ trọ lập quyết toán thanh lý hoàn cọc $\rightarrow$ Khách xác nhận quyết toán $\rightarrow$ Chủ trọ xác nhận chuyển khoản hoàn cọc thực tế $\rightarrow$ Hoàn tất quyết toán, hợp đồng chuyển `TERMINATED` và phòng trở lại trạng thái `TRONG`.

### 4.3. Chạy toàn bộ Test Suite (164 tests)
Chạy trong terminal riêng (dùng JDK 17):

```cmd
call scripts\mvn17.cmd clean test
```

Kết quả: **164/164 tests passed (0 failures, 0 errors, 0 skipped)**.

---

## 5. Cấu hình kết nối Cơ sở dữ liệu

File cấu hình: `src/main/resources/application-windows-auth.properties`
```properties
spring.datasource.url=jdbc:sqlserver://localhost:1435;databaseName=ApartmentDB_v2;integratedSecurity=true;encrypt=true;trustServerCertificate=true;
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.locations=classpath:db/migration/common,classpath:db/migration/sqlserver
app.seed.demo-rooms=false
app.db.migrate-unicode=false
```
