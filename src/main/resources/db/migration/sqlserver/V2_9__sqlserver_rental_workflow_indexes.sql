-- ============================================================================
-- V2_8__sqlserver_rental_workflow_and_workspaces.sql
-- Tạo các chỉ mục đặc thù (Filtered Unique Index, v.v.) trên SQL Server
-- ============================================================================

-- 1. Unique index cho yêu cầu xác nhận cọc đang chờ xử lý theo từng hợp đồng
CREATE UNIQUE INDEX UX_deposit_confirmation_requests_pending_contract
    ON dbo.deposit_confirmation_requests(contract_id)
    WHERE request_status = 'PENDING';

-- 2. Unique index cho payment_id trên deposit_confirmation_requests
CREATE UNIQUE INDEX UX_deposit_confirmation_requests_payment
    ON dbo.deposit_confirmation_requests(payment_id)
    WHERE payment_id IS NOT NULL;

-- 3. Chỉ mục tìm kiếm yêu cầu cọc theo chủ trọ & trạng thái
CREATE INDEX IX_deposit_confirmation_requests_landlord_status
    ON dbo.deposit_confirmation_requests(landlord_user_id, request_status, requested_at DESC);

-- 4. Unique index cho managed_room_id trên bài đăng
CREATE UNIQUE INDEX UX_room_listings_managed_room_id
    ON dbo.room_listings(managed_room_id)
    WHERE managed_room_id IS NOT NULL;

-- 5. Chỉ mục tìm kiếm trạng thái yêu cầu kết thúc hợp đồng
CREATE INDEX IX_termination_requests_contract_status
    ON dbo.contract_termination_requests(contract_id, status);

-- 6. Chỉ mục lịch sử kết thúc hợp đồng theo thời gian
CREATE INDEX IX_termination_histories_request_time
    ON dbo.contract_termination_histories(termination_request_id, happened_at);

-- 7. Chỉ mục thông báo người dùng theo trạng thái đọc
CREATE INDEX IX_user_notifications_recipient_read
    ON dbo.user_notifications(recipient_user_id, read_at, created_at DESC);

-- 8. Chỉ mục hóa đơn theo kỳ hợp đồng
CREATE INDEX IX_invoices_contract_period_start
    ON dbo.invoices(contract_id, period_start);

-- 9. Chỉ mục sự cố theo người báo
CREATE INDEX IX_incidents_reported_by_user
    ON dbo.incidents(reported_by_user_id, reported_at DESC);
