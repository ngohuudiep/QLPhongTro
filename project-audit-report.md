# Báo cáo Kiểm toán và Tích hợp Toàn diện QLPhongTro

**Thời điểm:** 2026-11-23 20:10 +07:00  
**Nhánh git:** `feature/integrate-workspaces`  
**Trạng thái:** HOÀN THÀNH TÍCH HỢP & XÁC THỰC RUNTIME 100%

---

## 1. Tổng quan tích hợp

Dự án đã tích hợp thành công toàn bộ các tính năng từ `incoming_source` (3 Workspaces: Admin, Landlord, Tenant; 2 Quy trình nghiệp vụ: Rental Workflow, Contract Termination; UI/UX Thymeleaf) vào nền tảng kiến trúc bảo mật chuẩn (RBAC, Scoped Access, Flyway Migration `ddl-auto=validate`) trên SQL Server thật (`ApartmentDB_v2`).

---

## 2. Chi tiết CSDL và Flyway Migration (V1 đến V2_14)

Hệ thống quản lý CSDL hoàn toàn qua Flyway, hỗ trợ kép cả SQL Server thật và H2 In-Memory:

### Tiến trình Migration:
- `V1__baseline_schema.sql`: 13 bảng cốt lõi kế thừa từ schema gốc.
- `V2_1` $\rightarrow$ `V2_7`: Bảo mật đăng nhập, khóa tài khoản, OTP, phân công nhân viên, chuẩn hóa vai trò, unique indexes.
- `V2_8__rental_workflow_and_workspaces.sql`: Bổ sung 6 bảng mới (`invoice_line_items`, `contract_termination_requests`, `contract_settlements`, `contract_termination_histories`, `deposit_confirmation_requests`, `user_notifications`) và các cột nghiệp vụ.
- `V2_9__sqlserver_rental_workflow_indexes.sql`: Chỉ mục hiệu năng cho các bảng nghiệp vụ.
- `V2_10__add_missing_contract_termination_columns.sql`: Khắc phục cột thiếu cho bảng `contract_termination_requests`.
- `V2_11__seed_roles_and_demo_data.sql`: Seed các vai trò hệ thống, 3 tài khoản demo chuẩn BCrypt (`admin`, `chutro`, `nguoithue`), tòa nhà và phòng mẫu.
- `V2_12__widen_status_columns.sql`: Mở rộng độ dài cột trạng thái (`VARCHAR(50)`) cho `booking_requests`, `contracts`, `contract_termination_requests`, `contract_settlements`.
- `V2_13__alter_contracts_nullable_columns.sql`: Cho phép `contracts.end_date` và `contracts.representative_id` nhận giá trị `NULL` trong giai đoạn tạo nháp hợp đồng.
- `V2_14__alter_payments_invoice_id_nullable.sql`: Cho phép `payments.invoice_id` nhận giá trị `NULL` cho các khoản thanh toán đặt cọc hợp đồng trực tuyến và mở rộng `payments.method VARCHAR(50)`.
- Hibernate cấu hình: `spring.jpa.hibernate.ddl-auto=validate` xác nhận khớp 100% giữa JPA Entity và schema Flyway.

---

## 3. Phân biệt Cơ sở Dữ liệu Cũ (`ApartmentDB`) và Mới (`ApartmentDB_v2`)

Trong hình ảnh chụp từ SQL Server Management Studio (SSMS) của người dùng:
1. **Database cũ (`ApartmentDB`)**:
   - Chứa 19 bảng với định dạng đặt tên hỗn hợp (PascalCase và snake_case như `dbo.Assets`, `dbo.Buildings`, `dbo.MeterReadings` và `dbo.meter_readings`, `dbo.RoomListings` và `dbo.room_listings`). Đây là tàn dư từ các lần chạy script thủ công trước đây, gây xung đột tên bảng và sai lệch ràng buộc.
2. **Database chuẩn hiện tại (`ApartmentDB_v2`)**:
   - Được tạo mới với Collation chuẩn tiếng Việt `Vietnamese_CI_AS`.
   - Chứa chính xác **24 bảng** hoàn toàn chuẩn hóa snake_case chữ thường (`assets`, `buildings`, `booking_requests`, `contracts`, `contract_settlements`, `contract_termination_requests`, `incidents`, `invoices`, `meter_readings`, `payments`, `roles`, `room_listings`, `rooms`, `users`...).
   - Tuyệt đối không có bảng trùng lặp hay xung đột hoa/thường.

---

## 4. Báo cáo Kiểm thử Tự động

- **Tổng số test cases:** 164
- **Passed:** 164 (100%)
- **Failures:** 0
- **Errors:** 0
- **Skipped:** 0
- **Build Result:** BUILD SUCCESS (Tổng thời gian: ~54 giây).
