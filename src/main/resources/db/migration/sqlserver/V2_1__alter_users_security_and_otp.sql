-- ============================================================================
-- V2_1__alter_users_security_and_otp.sql (SQL Server)
-- Thêm các cột bảo mật, khóa tài khoản và OTP cho bảng users
-- Có kiểm tra an toàn để tương thích với cơ sở dữ liệu đã có sẵn một phần cột
-- ============================================================================

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.users') AND name = 'email')
BEGIN
    ALTER TABLE dbo.users ADD email VARCHAR(100) NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.users') AND name = 'phone')
BEGIN
    ALTER TABLE dbo.users ADD phone VARCHAR(20) NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.users') AND name = 'failed_login_attempts')
BEGIN
    ALTER TABLE dbo.users ADD failed_login_attempts INT NULL DEFAULT 0;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.users') AND name = 'lockout_until')
BEGIN
    ALTER TABLE dbo.users ADD lockout_until DATETIME2 NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.users') AND name = 'account_enabled')
BEGIN
    ALTER TABLE dbo.users ADD account_enabled BIT NOT NULL DEFAULT 1;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.users') AND name = 'reset_otp')
BEGIN
    ALTER TABLE dbo.users ADD reset_otp VARCHAR(100) NULL;
END
ELSE
BEGIN
    -- Đảm bảo reset_otp đủ độ dài cho token
    ALTER TABLE dbo.users ALTER COLUMN reset_otp VARCHAR(100) NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.users') AND name = 'reset_otp_expiry')
BEGIN
    ALTER TABLE dbo.users ADD reset_otp_expiry DATETIME2 NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.users') AND name = 'reset_otp_failed_attempts')
BEGIN
    ALTER TABLE dbo.users ADD reset_otp_failed_attempts INT NULL DEFAULT 0;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.users') AND name = 'reset_otp_request_count')
BEGIN
    ALTER TABLE dbo.users ADD reset_otp_request_count INT NULL DEFAULT 0;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.users') AND name = 'reset_otp_request_window_start')
BEGIN
    ALTER TABLE dbo.users ADD reset_otp_request_window_start DATETIME2 NULL;
END;
