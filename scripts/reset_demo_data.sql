-- ====================================================================
-- SCRIPT RESET DỮ LIỆU DEMO QLPHONGTRO TRÊN SQL SERVER (ApartmentDB_v2)
-- Đảm bảo an toàn, lặp lại được (idempotent), không đụng ApartmentDB cũ
-- ====================================================================

SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

-- 1. Xóa các dữ liệu phát sinh từ kịch bản test tự động theo đúng thứ tự khóa ngoại
DELETE FROM dbo.user_notifications WHERE notification_id > 0;
DELETE FROM dbo.reviews WHERE review_id > 0;
DELETE FROM dbo.contract_settlements WHERE settlement_id > 0;
DELETE FROM dbo.contract_termination_histories WHERE history_id > 0;
DELETE FROM dbo.contract_termination_requests WHERE termination_request_id > 0;
DELETE FROM dbo.deposit_confirmation_requests WHERE request_id > 0;
DELETE FROM dbo.invoice_line_items WHERE line_item_id > 0;
DELETE FROM dbo.payments WHERE payment_id > 0;
DELETE FROM dbo.invoices WHERE invoice_id > 0;
DELETE FROM dbo.contract_tenants WHERE contract_id NOT IN (SELECT contract_id FROM dbo.contracts WHERE contract_code = 'HD-INIT-P103');
DELETE FROM dbo.contracts WHERE contract_code <> 'HD-INIT-P103';
DELETE FROM dbo.booking_requests WHERE request_id > 0;
DELETE FROM dbo.users WHERE username LIKE 'chutro_%';

-- 2. Đặt lại trạng thái tài khoản demo
UPDATE dbo.users 
SET account_status = 'APPROVED', account_enabled = 1, enabled = 1,
    failed_login_attempts = 0, lockout_until = NULL,
    average_rating = 0.00, total_reviews = 0, is_top_landlord = 0
WHERE username IN ('admin', 'chutro', 'nguoithue');

-- 3. Đặt lại trạng thái 2 phòng quản lý
UPDATE dbo.rooms SET status = 'TRONG', average_rating = 0.00, total_reviews = 0 WHERE room_code = 'P101';
UPDATE dbo.rooms SET status = 'DA_THUE', average_rating = 0.00, total_reviews = 0 WHERE room_code = 'P103';

-- 4. Đặt lại trạng thái 14 bài đăng phòng công khai chuẩn
UPDATE dbo.room_listings SET status = 'TRONG', current_tenant = NULL, tenant_phone = NULL, average_rating = 0.00, total_reviews = 0 WHERE room_code = 'P101';
UPDATE dbo.room_listings SET status = 'TRONG', current_tenant = NULL, tenant_phone = NULL, average_rating = 0.00, total_reviews = 0 WHERE room_code = 'P102';
UPDATE dbo.room_listings SET status = 'DA_THUE', current_tenant = N'Trần Hoàng Nam', tenant_phone = '0912345678', average_rating = 0.00, total_reviews = 0 WHERE room_code = 'P103';
UPDATE dbo.room_listings SET status = 'TRONG', current_tenant = NULL, tenant_phone = NULL, average_rating = 0.00, total_reviews = 0 WHERE room_code = 'P201';
UPDATE dbo.room_listings SET status = 'DA_THUE', current_tenant = N'Phạm Minh Tuấn', tenant_phone = '0923456789', average_rating = 0.00, total_reviews = 0 WHERE room_code = 'P202';
UPDATE dbo.room_listings SET status = 'TRONG', current_tenant = NULL, tenant_phone = NULL, average_rating = 0.00, total_reviews = 0 WHERE room_code = 'P203';
UPDATE dbo.room_listings SET status = 'TRONG', current_tenant = NULL, tenant_phone = NULL, average_rating = 0.00, total_reviews = 0 WHERE room_code = 'P301';
UPDATE dbo.room_listings SET status = 'DA_THUE', current_tenant = N'Lê Văn Long', tenant_phone = '0934567890', average_rating = 0.00, total_reviews = 0 WHERE room_code = 'P302';
UPDATE dbo.room_listings SET status = 'TRONG', current_tenant = NULL, tenant_phone = NULL, average_rating = 0.00, total_reviews = 0 WHERE room_code = 'P303';
UPDATE dbo.room_listings SET status = 'TRONG', current_tenant = NULL, tenant_phone = NULL, average_rating = 0.00, total_reviews = 0 WHERE room_code = 'P401';
UPDATE dbo.room_listings SET status = 'DA_THUE', current_tenant = N'Hoàng Thị Mai', tenant_phone = '0945678901', average_rating = 0.00, total_reviews = 0 WHERE room_code = 'P402';
UPDATE dbo.room_listings SET status = 'TRONG', current_tenant = NULL, tenant_phone = NULL, average_rating = 0.00, total_reviews = 0 WHERE room_code = 'P403';
UPDATE dbo.room_listings SET status = 'TRONG', current_tenant = NULL, tenant_phone = NULL, average_rating = 0.00, total_reviews = 0 WHERE room_code = 'P501';
UPDATE dbo.room_listings SET status = 'TRONG', current_tenant = NULL, tenant_phone = NULL, average_rating = 0.00, total_reviews = 0 WHERE room_code = 'P502';
