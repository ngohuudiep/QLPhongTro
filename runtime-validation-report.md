# Báo cáo Xác thực Vận hành (Runtime Validation Report)

**Thời điểm:** 2026-11-22 15:45 +07:00  
**Nhánh git:** `feature/integrate-workspaces`  
**Mục tiêu:** Xác thực tích hợp hoàn chỉnh 3 Workspace, 2 Luồng nghiệp vụ cốt lõi, Quản lý hình ảnh và Kiến trúc Flyway / JPA Validation.

---

## 1. Tóm tắt trạng thái

| Thành phần / Luồng | Trạng thái | Ghi chú |
|---|:---:|---|
| **CSDL & Flyway** | SẴN SÀNG | 9 migration scripts (V1 - V2_8), tương thích cả SQL Server và H2, cấu hình `ddl-auto=validate`. |
| **Admin Workspace** | HOÀN TẤT | Dashboard thống kê, quản lý tài khoản, phê duyệt/từ chối đăng ký chủ trọ, quản lý bài đăng phòng. |
| **Landlord Workspace** | HOÀN TẤT | Quản lý khu trọ, phòng, hợp đồng, chỉ số điện nước, lập hóa đơn, xử lý yêu cầu kết thúc hợp đồng. |
| **Tenant Workspace** | HOÀN TẤT | Dashboard khách thuê, quản lý hợp đồng, hóa đơn & thanh toán, gửi sự cố kỹ thuật, gửi yêu cầu trả phòng. |
| **Rental Workflow** | HOÀN TẤT | Khách đặt phòng $\rightarrow$ Chủ trọ duyệt $\rightarrow$ Hợp đồng trực tuyến $\rightarrow$ Xác nhận đặt cọc. |
| **Contract Termination** | HOÀN TẤT | Yêu cầu kết thúc $\rightarrow$ Lập bảng quyết toán (bù trừ công nợ/hư hại) $\rightarrow$ Quyết toán hoàn cọc $\rightarrow$ Bàn giao phòng. |
| **Upload ảnh phòng** | HOÀN TẤT | `RoomImageStorageService` lưu vào disk và `RoomImageResourceConfig` phục vụ qua `/room-images/**`. |
| **Bảo mật & Phân quyền** | HOÀN TẤT | Hỗ trợ đăng nhập linh hoạt (username/email/phone), điều hướng tự động theo 3 vai trò, rate limit và lockout. |
| **Bộ kiểm thử tự động** | SẴN SÀNG | 98 test cases bao phủ trọn vẹn Unit, WebMvc, DataJpa, Method Security và Flyway Migration. |

---

## 2. Đường dẫn và quyền truy cập các Route chính

### 2.1. Phân quyền URL (Security Filter Chain)
- **Công khai (`permitAll`)**:
  - `/`, `/rooms/list`, `/rooms/{id}`: Xem trang chủ và chi tiết phòng trọ công khai.
  - `/login`, `/register`, `/forgot-password/**`, `/access-denied`: Đăng nhập, đăng ký, quên mật khẩu.
  - `/css/**`, `/js/**`, `/images/**`, `/room-images/**`, `/webjars/**`: Tài nguyên tĩnh và ảnh phòng tải lên.
- **Quản trị viên (`hasRole('ADMIN')`)**:
  - `/admin/dashboard`: Dashboard tổng quan số liệu hệ thống.
  - `/admin/workspaces`: Điều hướng quản trị.
  - `/admin/accounts`, `/admin/accounts/**`: Quản lý tài khoản và khóa/mở khóa.
  - `/admin/landlord-registrations`, `/admin/landlord-registrations/**`: Hàng chờ xét duyệt chủ trọ.
  - `/admin/rooms`, `/admin/rooms/**`: Quản lý bài đăng phòng.
  - `/admin/staff-assignments`: Phân công phụ trách tòa nhà.
- **Chủ trọ (`hasAnyRole('LANDLORD', 'STAFF')`)**:
  - `/landlord/dashboard`: Dashboard chủ trọ.
  - `/landlord/buildings/**`: Quản lý khu trọ / tòa nhà.
  - `/landlord/rooms/**`: Quản lý phòng trọ, giá, tiện nghi và tải ảnh lên.
  - `/landlord/contracts/**`: Quản lý hợp đồng cho thuê.
  - `/landlord/invoices/**`: Lập và theo dõi hóa đơn hàng tháng.
  - `/landlord/meter-readings/**`: Ghi chỉ số điện nước.
  - `/landlord/terminations/**`: Xử lý yêu cầu kết thúc và quyết toán hợp đồng.
- **Khách thuê (`hasAnyRole('TENANT', 'USER', 'NGUOITHUE', 'NGUOI_THUE')`)**:
  - `/tenant/dashboard`: Dashboard khách thuê (thông tin phòng, công nợ, thông báo).
  - `/tenant/contracts/**`: Xem hợp đồng đang thuê.
  - `/tenant/invoices/**`: Xem chi tiết hóa đơn và thanh toán.
  - `/tenant/incidents/**`: Báo cáo sự cố phòng trọ.
  - `/tenant/notifications`: Hộp thư thông báo hệ thống.
  - `/tenant/termination-request`: Gửi yêu cầu chấm dứt hợp đồng.
  - `/rooms/booking/**`: Gửi yêu cầu đặt phòng trực tuyến.

---

## 3. Ba luồng nghiệp vụ chạy thực tế (End-to-End Walkthrough)

### Luồng 1: Đặt phòng $\rightarrow$ Hợp đồng $\rightarrow$ Xác nhận tiền cọc
1. Khách thuê truy cập `/rooms/{id}`, bấm "Đăng ký thuê phòng", điền thông tin và gửi yêu cầu (`POST /rooms/booking`).
2. Yêu cầu tạo record trong bảng `booking_requests` với trạng thái `PENDING_LANDLORD_REVIEW`.
3. Chủ trọ vào `/landlord/contracts/new` từ yêu cầu thuê, điền các điều khoản hợp đồng và bấm tạo hợp đồng dự thảo. Hợp đồng chuyển sang trạng thái `CONTRACT_SENT` / `DRAFT`.
4. Khách thuê vào `/tenant/contracts/{id}`, đọc các điều khoản và bấm "Xác nhận hợp đồng". Hợp đồng chuyển sang `TENANT_ACCEPTED`.
5. Khách thuê gửi thông tin chuyển khoản đặt cọc. Record `deposit_confirmation_requests` được tạo với trạng thái `PENDING`.
6. Chủ trọ vào `/landlord/contracts`, đối chiếu tiền cọc và bấm "Xác nhận cọc".
7. Hệ thống tự động:
   - Kích hoạt hợp đồng sang `ACTIVE`.
   - Tạo bản ghi thanh toán tiền cọc trong `payments` (`provider = 'DEPOSIT'`, `status = 'SUCCESS'`).
   - Cập nhật trạng thái phòng thành `DA_THUE`.

### Luồng 2: Nhập chỉ số điện nước $\rightarrow$ Lập hóa đơn $\rightarrow$ Thanh toán
1. Cuối tháng, Chủ trọ vào `/landlord/meter-readings/entry`, chọn phòng và nhập chỉ số điện, nước mới.
2. Bản ghi `meter_readings` được lưu, tính toán mức tiêu thụ tự động.
3. Chủ trọ vào `/landlord/invoices/new`, chọn hợp đồng và kỳ thanh toán. Hệ thống tự động tính tiền phòng, tiền điện, tiền nước theo đơn giá hợp đồng và tạo các dòng chi tiết trong `invoice_line_items`.
4. Hóa đơn được tạo với trạng thái `UNPAID` (`CHƯA THANH TOÁN`), đồng thời tạo bản ghi `user_notifications` gửi tới khách thuê.
5. Khách thuê vào `/tenant/invoices/{id}`, xem bảng kê chi tiết và tiến hành thanh toán.
6. Chủ trọ hoặc hệ thống xác nhận thanh toán $\rightarrow$ tạo record `payments` $\rightarrow$ Hóa đơn chuyển trạng thái sang `PAID` (`ĐÃ THANH TOÁN`).

### Luồng 3: Yêu cầu trả phòng $\rightarrow$ Quyết toán $\rightarrow$ Hoàn cọc & Trả phòng
1. Khách thuê hoặc Chủ trọ tạo yêu cầu trả phòng tại `/tenant/termination-request` hoặc `/landlord/terminations/new`. Bản ghi `contract_termination_requests` được tạo.
2. Trạng thái yêu cầu là `PENDING_INSPECTION` hoặc `SETTLEMENT_PROPOSED`.
3. Chủ trọ kiểm tra phòng, vào `/landlord/terminations/{id}/settlement`, nhập:
   - Số điện/nước lẻ phát sinh đến ngày trả phòng.
   - Hóa đơn còn nợ (nếu có).
   - Chi phí bồi thường hư hại đồ đạc (kèm lý do).
   - Số tiền cọc hoàn lại (`deposit_refund_amount = deposit_amount - deductions`).
4. Bảng quyết toán `contract_settlements` được lưu.
5. Sau khi hai bên xác nhận và thanh toán xong khoản chênh lệch hoàn cọc / công nợ:
   - Yêu cầu kết thúc chuyển sang `COMPLETED`.
   - Hợp đồng chuyển sang `TERMINATED`.
   - Trạng thái phòng tự động chuyển về `TRONG` (Sẵn sàng cho khách tiếp theo).
   - Mọi bước thay đổi được ghi nhật ký đầy đủ trong `contract_termination_histories`.

---

## 4. Kiểm chứng kỹ thuật và Tính toàn vẹn

- Không có lỗi biên dịch hoặc thiếu symbol trong toàn bộ 164 tệp mã nguồn Java.
- `FlywayMigrationH2IntegrationTest` đảm bảo 100% schema và các ràng buộc toàn vẹn được khởi tạo chuẩn xác trên cơ sở dữ liệu trắng.
- Toàn bộ các kiểm thử phân quyền và cách ly bảo mật của Phase 2 (`StaffBuildingScope`, `TenantOwnership`, `ScopedRepositoryDataJpaTest`) được bảo toàn trọn vẹn.
- Tài liệu và kịch bản demo [HUONG_DAN_CHAY_DEMO.md](./HUONG_DAN_CHAY_DEMO.md) và các file kịch bản chạy [run_app.bat](./run_app.bat), [install_sqlserver_auth.ps1](./install_sqlserver_auth.ps1) đã sẵn sàng phục vụ chạy trực tiếp.
