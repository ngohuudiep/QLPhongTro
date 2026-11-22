-- ============================================================================
-- V2_8__h2_rental_workflow_and_workspaces.sql
-- Tạo các chỉ mục tiêu chuẩn tương đương trên H2 (H2 không hỗ trợ mệnh đề WHERE trong CREATE INDEX)
-- ============================================================================

CREATE INDEX IX_deposit_confirmation_requests_landlord_status
    ON deposit_confirmation_requests(landlord_user_id, request_status, requested_at);

CREATE INDEX IX_termination_requests_contract_status
    ON contract_termination_requests(contract_id, status);

CREATE INDEX IX_termination_histories_request_time
    ON contract_termination_histories(termination_request_id, happened_at);

CREATE INDEX IX_user_notifications_recipient_read
    ON user_notifications(recipient_user_id, read_at, created_at);

CREATE INDEX IX_invoices_contract_period_start
    ON invoices(contract_id, period_start);

CREATE INDEX IX_incidents_reported_by_user
    ON incidents(reported_by_user_id, reported_at);
