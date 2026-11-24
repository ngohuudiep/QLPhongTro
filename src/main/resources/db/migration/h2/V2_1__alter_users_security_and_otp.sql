-- ============================================================================
-- V2_1__alter_users_security_and_otp.sql (H2)
-- Thêm các cột bảo mật, khóa tài khoản và OTP cho bảng users
-- ============================================================================

ALTER TABLE users ADD IF NOT EXISTS email VARCHAR(100) NULL;
ALTER TABLE users ADD IF NOT EXISTS phone VARCHAR(20) NULL;
ALTER TABLE users ADD IF NOT EXISTS failed_login_attempts INT NULL DEFAULT 0;
ALTER TABLE users ADD IF NOT EXISTS lockout_until DATETIME2 NULL;
ALTER TABLE users ADD IF NOT EXISTS account_enabled BIT NOT NULL DEFAULT 1;
ALTER TABLE users ADD IF NOT EXISTS reset_otp VARCHAR(100) NULL;
ALTER TABLE users ADD IF NOT EXISTS reset_otp_expiry DATETIME2 NULL;
ALTER TABLE users ADD IF NOT EXISTS reset_otp_failed_attempts INT NULL DEFAULT 0;
ALTER TABLE users ADD IF NOT EXISTS reset_otp_request_count INT NULL DEFAULT 0;
ALTER TABLE users ADD IF NOT EXISTS reset_otp_request_window_start DATETIME2 NULL;
