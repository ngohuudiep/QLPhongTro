# Báo cáo Kiểm toán và Tích hợp Toàn diện QLPhongTro

**Thời điểm:** 2026-11-22 15:45 +07:00  
**Nhánh git:** `feature/integrate-workspaces`  
**Trạng thái:** HOÀN THÀNH TÍCH HỢP (READY FOR DEMO & VERIFICATION)

---

## 1. Tổng quan tích hợp

Dự án đã tích hợp thành công toàn bộ các tính năng từ `incoming_source` (Workspaces, Rental Workflow, Contract Termination, UI/UX Thymeleaf) vào nền tảng kiến trúc bảo mật chuẩn (RBAC, Scoped Access, Flyway Migration `ddl-auto=validate`).

Toàn bộ hệ thống hiện bao gồm:
1. **3 Không gian làm việc chuyên biệt (Workspaces)**:
   - **Admin Workspace** (`/admin/dashboard`, `/admin/workspaces`, `/admin/accounts`, `/admin/landlord-registrations`, `/admin/rooms`, `/admin/staff-assignments`):
     Quản lý thống kê toàn hệ thống, danh sách tài khoản người dùng, hàng chờ xét duyệt Chủ trọ, quản lý bài đăng phòng và phân công phụ trách tòa nhà.
   - **Landlord Workspace** (`/landlord/dashboard`, `/landlord/workspaces`, `/landlord/buildings`, `/landlord/rooms`, `/landlord/contracts`, `/landlord/invoices`, `/landlord/meter-readings`, `/landlord/terminations`):
     Không gian vận hành của Chủ trọ: quản lý khu trọ, tạo & duyệt phòng trọ, lập hợp đồng, nhập chỉ số điện nước, xuất hóa đơn và xử lý kết thúc hợp đồng.
   - **Tenant Workspace / Portal** (`/tenant/dashboard`, `/tenant/workspaces`, `/tenant/contracts`, `/tenant/invoices`, `/tenant/incidents`, `/tenant/notifications`, `/tenant/termination-request`):
     Cổng thông tin khách thuê: theo dõi trạng thái hợp đồng, xem & xác nhận hóa đơn, thanh toán, phản ánh sự cố kỹ thuật và gửi yêu cầu trả phòng.

2. **2 Quy trình nghiệp vụ cốt lõi (Workflows)**:
   - **Rental Workflow (Đặt phòng $\rightarrow$ Hợp đồng $\rightarrow$ Xác nhận cọc)**:
     Khách thuê gửi yêu cầu đặt phòng (`BookingRequest`) $\rightarrow$ Chủ trọ duyệt và tạo hợp đồng dự thảo $\rightarrow$ Khách thuê xác nhận hợp đồng trực tuyến $\rightarrow$ Khách gửi yêu cầu xác nhận tiền cọc $\rightarrow$ Chủ trọ xác nhận cọc (`DepositConfirmationRequest`) $\rightarrow$ Kích hoạt hợp đồng và đánh dấu phòng `DA_THUE`.
   - **Contract Termination & Settlement (Yêu cầu kết thúc $\rightarrow$ Quyết toán $\rightarrow$ Hoàn cọc)**:
     Bên thuê hoặc bên cho thuê tạo yêu cầu kết thúc (`ContractTerminationRequest`) $\rightarrow$ Chủ trọ kiểm tra phòng và lập bảng quyết toán (`ContractSettlement`: cấn trừ hóa đơn chưa thanh toán, tiền điện nước lẻ, tiền phạt hư hỏng) $\rightarrow$ Khách thuê xác nhận $\rightarrow$ Hoàn tất hoàn cọc/thanh toán $\rightarrow$ Chuyển trạng thái hợp đồng thành `TERMINATED` và giải phóng phòng thành `TRONG`.

3. **Quản lý Hình ảnh phòng trọ (Image Storage)**:
   - Dịch vụ `RoomImageStorageService` lưu trữ ảnh phòng vào thư mục máy chủ (`${user.home}/qlphongtro-data/room-images`).
   - Cấu hình tài nguyên `RoomImageResourceConfig` phục vụ ảnh qua URL `/room-images/**`.

---

## 2. Chi tiết CSDL và Flyway Migration (V1 đến V2_8)

Hệ thống quản lý CSDL hoàn toàn qua Flyway, chia tách thư mục theo `{vendor}`:
- `classpath:db/migration/common`: ANSI SQL tương thích cả SQL Server và H2.
- `classpath:db/migration/sqlserver`: Tối ưu riêng cho SQL Server (Filtered Unique Indexes `WHERE ... IS NOT NULL`, `WHERE request_status = 'PENDING'`).
- `classpath:db/migration/h2`: Chỉ mục tiêu chuẩn chạy trên H2 In-Memory mode MSSQLServer.

### Tiến trình Migration:
- `V1__baseline_schema.sql`: 13 bảng cốt lõi kế thừa từ schema gốc.
- `V2_1__alter_users_security_and_otp.sql`: Bảo mật đăng nhập, khóa tài khoản, OTP và reset password.
- `V2_2__alter_tenants_and_incidents_add_fk.sql`: Khóa ngoại người dùng và sự cố.
- `V2_3__create_staff_building_assignments.sql`: Phân công nhân viên quản lý tòa nhà.
- `V2_4__alter_room_listings_add_columns.sql`: Bổ sung cột quản lý bài đăng phòng.
- `V2_5__alter_roles_system_status.sql`: Chuẩn hóa vai trò hệ thống.
- `V2_6__sqlserver_filtered_uniques_and_fk_cleanup.sql` / `V2_6__h2_nullable_uniques.sql`: Ràng buộc Unique lọc dữ liệu.
- `V2_7__link_room_listings_to_rooms.sql`: Liên kết bài đăng phòng với phòng vật lý (`room_id`).
- `V2_8__rental_workflow_and_workspaces.sql`: Bổ sung 6 bảng mới (`invoice_line_items`, `contract_termination_requests`, `contract_settlements`, `contract_termination_histories`, `deposit_confirmation_requests`, `user_notifications`) và các cột nghiệp vụ cho `users`, `buildings`, `booking_requests`, `contracts`, `payments`, `invoices`, `incidents`, `room_listings`.
- Hibernate cấu hình: `spring.jpa.hibernate.ddl-auto=validate` xác nhận khớp 100% giữa JPA Entity và schema Flyway.

---

## 3. Chiến lược đồng bộ Vai trò & Bảo mật (RBAC)

Để tương thích tuyệt đối giữa mã nguồn RBAC Phase 2 (dùng `ADMIN`, `STAFF`, `USER`) và mã nguồn Workspaces (dùng `ADMIN`, `LANDLORD`, `TENANT`):
- `Role.java`: Định nghĩa đầy đủ các hằng số vai trò và hàm `normalizeRoleName(...)`.
- `CustomUserDetailsService`: Khi cấp quyền (GrantedAuthority), tài khoản `STAFF`/`LANDLORD` nhận đồng thời cả `ROLE_LANDLORD` và `ROLE_STAFF`; tài khoản `USER`/`TENANT` nhận đồng thời cả `ROLE_TENANT` và `ROLE_USER`.
- `SecurityConfig`:
  - Đăng nhập thành công điều hướng tự động: Admin $\rightarrow$ `/admin/dashboard`, Chủ trọ $\rightarrow$ `/landlord/dashboard`, Khách thuê $\rightarrow$ `/tenant/dashboard`.
  - Bảo vệ đa tầng: Phân quyền URL tầng filter (`requestMatchers`) kết hợp `@PreAuthorize` và kiểm tra quyền sở hữu dữ liệu tầng Service (`BuildingAccess`, `CurrentUser`).

---

## 4. Danh mục Kiểm thử (Test Suite)

Dự án hiện có tổng cộng **98 test cases** tự động, bao gồm:
1. `FlywayMigrationH2IntegrationTest`: Xác minh tự động chạy trơn tru từ V1 đến V2_8 trên DB trắng và Hibernate validate toàn bộ Entity.
2. `ScopedRepositoryDataJpaTest`, `StaffBuildingScopeMockMvcIntegrationTest`, `ServiceMethodSecurityIntegrationTest`: Kiểm tra phân quyền phạm vi tòa nhà của Staff.
3. `TenantSecurityTest`, `TenantOwnershipServiceTest`, `InvoiceOwnershipMockMvcTest`, `ContractOwnershipMockMvcTest`: Kiểm tra cách ly dữ liệu giữa các người thuê và chủ trọ.
4. `AdminLandlordRegistrationViewTest`, `AdminWorkspaceServiceTest`: Kiểm tra luồng duyệt chủ trọ và quản trị tài khoản.
5. `LandlordWorkspaceControllerViewTest`, `LandlordWorkspaceServiceTest`: Kiểm tra nghiệp vụ chủ trọ.
6. `TenantPortalServiceImplTest`: Kiểm tra cổng thông tin khách thuê.
7. `RentalWorkflowControllerTest`, `RentalWorkflowServiceImplTest`: Kiểm tra luồng đặt phòng và cọc.
8. `ContractTerminationServiceImplTest`: Kiểm tra luồng thanh lý và quyết toán hợp đồng.
9. `RoomImageStorageServiceTest`: Kiểm tra lưu trữ và kiểm định tệp ảnh phòng.
10. `SecurityConfigTest`, `UserRegistrationRoleTest`, `AuthControllerTest`, `AuthenticationEventsTest`: Kiểm tra đăng nhập, đăng ký và bảo mật xác thực.

---

## 5. Hướng dẫn kiểm tra và Chạy thử nghiệm

Người vận hành có thể chạy kiểm chứng toàn bộ test và ứng dụng trên PowerShell:
```powershell
# Chạy toàn bộ test
.\scripts\mvn17.cmd "-Dmaven.repo.local=.m2repo" -o clean test

# Khởi chạy ứng dụng (profile mặc định với SQL Server)
.\scripts\mvn17.cmd "-Dmaven.repo.local=.m2repo" -o spring-boot:run
```
Hoặc chạy thử nghiệm nhanh theo hướng dẫn trong [HUONG_DAN_CHAY_DEMO.md](./HUONG_DAN_CHAY_DEMO.md) với `run_app.bat`.
