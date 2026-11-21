-- ============================================================================
-- V2_1__alter_users_security_and_otp.sql
-- Thêm các cột bảo mật, khóa tài khoản và OTP cho bảng users
-- Cú pháp ANSI SQL tương thích cả SQL Server và H2
-- ============================================================================

ALTER TABLE users ADD email VARCHAR(100) NULL;
ALTER TABLE users ADD phone VARCHAR(20) NULL;
ALTER TABLE users ADD failed_login_attempts INT NULL DEFAULT 0;
ALTER TABLE users ADD lockout_until DATETIME2 NULL;
ALTER TABLE users ADD account_enabled BIT NOT NULL DEFAULT 1;
ALTER TABLE users ADD reset_otp VARCHAR(100) NULL;
ALTER TABLE users ADD reset_otp_expiry DATETIME2 NULL;
ALTER TABLE users ADD reset_otp_failed_attempts INT NULL DEFAULT 0;
ALTER TABLE users ADD reset_otp_request_count INT NULL DEFAULT 0;
ALTER TABLE users ADD reset_otp_request_window_start DATETIME2 NULL;
