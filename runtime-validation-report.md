# Báo cáo Xác thực Vận hành Thực tế (Runtime Validation Report)

**Thời điểm:** 2026-11-23 20:10 +07:00  
**Nhánh git:** `feature/integrate-workspaces`  
**Môi trường thực thi:**
- **JDK:** Java 17.0.11 (`C:\Program Files\Java\jdk-17`)
- **SQL Server:** `LAPTOP-12OGD3V1\SQLEXPRESS01`, port `1435`
- **Database:** `ApartmentDB_v2` (Collation: `Vietnamese_CI_AS`)
- **Profile:** `windows-auth` (`integratedSecurity=true`)
- **Flyway Status:** `v2.14` sạch, `ddl-auto=validate` qua 100%

---

## 1. Kết quả kiểm chứng chi tiết

| Hạng mục kiểm tra | Trạng thái | Bằng chứng thực tế (Log / Output) |
|---|:---:|---|
| **1. Khởi động với DB trống & Tính lặp lại** | **PASS** | Đã kiểm chứng trên DB trống thứ 2 `ApartmentDB_test`: Flyway V1 $\rightarrow$ V2_14 chạy 100% thành công, Hibernate validate khớp hoàn toàn. |
| **2. Seed vai trò & 3 tài khoản Demo** | **PASS** | `V2_11__seed_roles_and_demo_data.sql` tạo đủ 3 tài khoản (`admin`, `chutro`, `nguoithue`), mật khẩu BCrypt, kích hoạt hoạt động ngay. |
| **3. Live Smoke Tests (3 vai trò)** | **PASS** | `admin.mjs` $\rightarrow$ `/admin/dashboard` (200 OK)<br>`landlord.mjs` $\rightarrow$ `/landlord/dashboard` (200 OK)<br>`tenant.mjs` $\rightarrow$ `/tenant/dashboard` (200 OK). |
| **4. Hỗ trợ tiếng Việt Unicode** | **PASS** | Truy vấn SQL hiển thị đúng dấu tiếng Việt có dấu. Trang `/rooms/list` render chính xác các chuỗi Unicode như: *"Phòng Studio Ban công thoáng mát, Full nội thất"*, *"Chuyển địa điểm làm việc sang quận khác"*. |
| **5. Luồng A: Đặt phòng $\rightarrow$ Hợp đồng $\rightarrow$ Duyệt cọc** | **PASS** | Tenant gửi booking $\rightarrow$ Landlord duyệt $\rightarrow$ Tạo draft $\rightarrow$ Tenant ký online $\rightarrow$ Tenant gửi deposit request $\rightarrow$ Landlord duyệt cọc $\rightarrow$ Hợp đồng chuyển `ACTIVE`. |
| **6. Luồng B: Chỉ số $\rightarrow$ Hóa đơn $\rightarrow$ Thanh toán** | **PASS** | Landlord chốt chỉ số điện/nước $\rightarrow$ Lập hóa đơn $\rightarrow$ Xác nhận thanh toán (`confirm-paid`) $\rightarrow$ Hóa đơn chuyển trạng thái `PAID`. |
| **7. Luồng D: Báo cáo sự cố, Duyệt chủ trọ, Khóa tài khoản** | **PASS** | Tenant gửi sự cố kèm trường mô tả `description` lưu vào DB $\rightarrow$ Khách đăng ký tài khoản chủ trọ mới $\rightarrow$ Admin duyệt $\rightarrow$ Admin khóa và mở khóa tài khoản người thuê thành công. |
| **8. Luồng C: Chấm dứt Hợp đồng $\rightarrow$ Quyết toán $\rightarrow$ Đóng hợp đồng** | **PASS** | Tenant gửi yêu cầu hủy hợp đồng $\rightarrow$ Landlord duyệt $\rightarrow$ Landlord lập bảng quyết toán $\rightarrow$ Tenant xác nhận quyết toán $\rightarrow$ Landlord xác nhận hoàn cọc thực tế $\rightarrow$ Hợp đồng chuyển `TERMINATED`, phòng chuyển về `TRONG`. |
| **9. Toàn bộ Test Suite (`mvn clean test`)** | **PASS** | **Tests run: 164, Failures: 0, Errors: 0, Skipped: 0**<br>**BUILD SUCCESS** (54.446 s). |

---

## 2. Bằng chứng thực tế các kịch bản chạy

### 2.1. Kết quả chạy kịch bản 4 luồng nghiệp vụ liên hoàn (`verify-all-business-flows.mjs`)

```text
=== STARTING FULL BUSINESS FLOW VERIFICATION ===

--- LUỒNG A: Đặt phòng -> Hợp đồng -> Xác nhận cọc ---
✓ Tenant đã gửi yêu cầu thuê phòng thành công
✓ Chủ trọ duyệt yêu cầu #6, tạo bản nháp hợp đồng #3
✓ Chủ trọ đã gửi hợp đồng #3 cho người thuê
✓ Khách thuê đã ký chấp nhận hợp đồng #3
✓ Khách thuê đã gửi yêu cầu xác nhận tiền cọc cho hợp đồng #3
✓ Chủ trọ đã duyệt tiền cọc #2 -> Hợp đồng #3 chính thức có hiệu lực (ACTIVE)

--- LUỒNG B: Ghi chỉ số -> Lập hóa đơn -> Thanh toán ---
✓ Chủ trọ đã chốt chỉ số điện nước thành công
✓ Hóa đơn #1 đã được ghi nhận thanh toán thành công (PAID)

--- LUỒNG D: Sự cố Tenant, Admin duyệt đăng ký chủ trọ & Khóa tài khoản ---
✓ Khách thuê đã gửi phản ánh sự cố kèm mô tả chi tiết thành công
✓ Người dùng đã đăng ký tài khoản chủ trọ mới: chutro_4421 (ở trạng thái chờ duyệt)
✓ Admin đã duyệt thành công tài khoản chủ trọ #9 (chutro_4421)
✓ Admin đã khóa tài khoản người thuê #3 (nguoithue)
✓ Admin đã mở khóa tài khoản người thuê #3 (nguoithue)

--- LUỒNG C: Khách yêu cầu kết thúc -> Quyết toán hoàn cọc ---
✓ Khách thuê đã gửi yêu cầu kết thúc hợp đồng #3 (yêu cầu #2)
✓ Chủ trọ đã phê duyệt yêu cầu kết thúc #2
✓ Chủ trọ đã lập bảng quyết toán hoàn cọc #2
✓ Khách thuê đã xác nhận đồng ý quyết toán #2
✓ Chủ trọ đã xác nhận hoàn tiền cọc thực tế (mã giao dịch: REFUND-VNPAY-20261231-01)
✓ Chủ trọ đã hoàn tất quyết toán và đóng hợp đồng #3 thành công

=======================================================
TẤT CẢ CÁC LUỒNG NGHIỆP VỤ A, B, C, D ĐÃ CHẠY THÀNH CÔNG 100%!
=======================================================
```

### 2.2. Kết quả chạy toàn bộ kiểm thử tự động (`mvn clean test`)

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
...
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in com.qlphongtro.VietnameseUnicodeSupportIntegrationTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 164, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  54.446 s
[INFO] Finished at: 2026-11-23T20:11:06+07:00
[INFO] ------------------------------------------------------------------------
```
