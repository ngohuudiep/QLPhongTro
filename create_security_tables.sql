-- ====================================================================
-- Script tạo và cập nhật bảng Roles, Users, RoomListings cho hệ thống
-- Chạy trong SQL Server Management Studio (SSMS) hoặc sqlcmd
-- ====================================================================

USE ApartmentDB;
GO

-- ================================================
-- 1. Bảng Roles - Lưu vai trò người dùng
-- ================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'Roles' AND type = 'U')
BEGIN
    CREATE TABLE Roles (
        role_id    INT IDENTITY(1,1) PRIMARY KEY,
        role_name  VARCHAR(20) NOT NULL UNIQUE  -- ADMIN, STAFF, USER
    );
    PRINT '>>> Da tao bang Roles';
END
GO

-- ================================================
-- 2. Bảng Users - Lưu tài khoản người dùng
-- ================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'Users' AND type = 'U')
BEGIN
    CREATE TABLE Users (
        user_id                INT IDENTITY(1,1) PRIMARY KEY,
        username               VARCHAR(50)  NOT NULL UNIQUE,
        password               VARCHAR(255) NOT NULL,         -- BCrypt hash
        full_name              NVARCHAR(150),
        email                  VARCHAR(100) UNIQUE,
        phone                  VARCHAR(20) UNIQUE,
        failed_login_attempts  INT DEFAULT 0,
        lockout_until          DATETIME2,
        reset_otp              VARCHAR(10),
        reset_otp_expiry       DATETIME2,
        role_id                INT NOT NULL,
        CONSTRAINT FK_Users_Roles FOREIGN KEY (role_id) REFERENCES Roles(role_id)
    );
    PRINT '>>> Da tao bang Users';
END
ELSE
BEGIN
    -- Cập nhật cột bổ sung nếu bảng Users đã tồn tại từ trước
    IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Users') AND name = 'email')
        ALTER TABLE Users ADD email VARCHAR(100);
    IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Users') AND name = 'phone')
        ALTER TABLE Users ADD phone VARCHAR(20);
    IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Users') AND name = 'failed_login_attempts')
        ALTER TABLE Users ADD failed_login_attempts INT DEFAULT 0;
    IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Users') AND name = 'lockout_until')
        ALTER TABLE Users ADD lockout_until DATETIME2;
    IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Users') AND name = 'reset_otp')
        ALTER TABLE Users ADD reset_otp VARCHAR(10);
    IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Users') AND name = 'reset_otp_expiry')
        ALTER TABLE Users ADD reset_otp_expiry DATETIME2;
END
GO

-- ================================================
-- 3. Bảng RoomListings - Bài đăng phòng trọ
-- ================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'RoomListings' AND type = 'U')
BEGIN
    CREATE TABLE RoomListings (
        listing_id          INT IDENTITY(1,1) PRIMARY KEY,
        room_code           VARCHAR(20)    NOT NULL UNIQUE,
        title               NVARCHAR(255)  NOT NULL,
        description         NVARCHAR(MAX),
        floor_no            INT DEFAULT 1,
        area                DECIMAL(8, 2)  NOT NULL,
        price               DECIMAL(14, 2) NOT NULL,
        status              VARCHAR(15)    NOT NULL DEFAULT 'TRONG',  -- TRONG | DA_THUE | BAO_TRI
        amenities           NVARCHAR(500),
        image_url           VARCHAR(500),
        image_gallery       NVARCHAR(MAX),
        current_tenant      NVARCHAR(150),
        tenant_phone        VARCHAR(20),
        lease_start_date    DATE,
        lease_end_date      DATE,
        created_at          DATETIME2 DEFAULT GETDATE(),
        created_by_user_id  INT,
        CONSTRAINT FK_RoomListings_Users FOREIGN KEY (created_by_user_id) REFERENCES Users(user_id),
        CONSTRAINT CK_Status CHECK (status IN ('TRONG', 'DA_THUE', 'BAO_TRI'))
    );
    PRINT '>>> Da tao bang RoomListings';
END
ELSE
BEGIN
    -- Cập nhật cột bổ sung nếu bảng RoomListings đã tồn tại
    IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('RoomListings') AND name = 'floor_no')
        ALTER TABLE RoomListings ADD floor_no INT DEFAULT 1;
    IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('RoomListings') AND name = 'amenities')
        ALTER TABLE RoomListings ADD amenities NVARCHAR(500);
    IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('RoomListings') AND name = 'image_gallery')
        ALTER TABLE RoomListings ADD image_gallery NVARCHAR(MAX);
    IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('RoomListings') AND name = 'current_tenant')
        ALTER TABLE RoomListings ADD current_tenant NVARCHAR(150);
    IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('RoomListings') AND name = 'tenant_phone')
        ALTER TABLE RoomListings ADD tenant_phone VARCHAR(20);
    IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('RoomListings') AND name = 'lease_start_date')
        ALTER TABLE RoomListings ADD lease_start_date DATE;
    IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('RoomListings') AND name = 'lease_end_date')
        ALTER TABLE RoomListings ADD lease_end_date DATE;
    IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('RoomListings') AND name = 'created_at')
        ALTER TABLE RoomListings ADD created_at DATETIME2 DEFAULT GETDATE();
END
GO

-- ================================================
-- 4. Dữ liệu mẫu - Roles
-- ================================================
IF NOT EXISTS (SELECT 1 FROM Roles WHERE role_name = 'ADMIN')
    INSERT INTO Roles (role_name) VALUES ('ADMIN');
IF NOT EXISTS (SELECT 1 FROM Roles WHERE role_name = 'STAFF')
    INSERT INTO Roles (role_name) VALUES ('STAFF');
IF NOT EXISTS (SELECT 1 FROM Roles WHERE role_name = 'USER')
    INSERT INTO Roles (role_name) VALUES ('USER');
GO

-- ================================================
-- 5. Dữ liệu mẫu - Users
-- Mật khẩu mặc định: 123456 (BCrypt hash)
-- ================================================
IF NOT EXISTS (SELECT 1 FROM Users WHERE username = 'admin')
BEGIN
    INSERT INTO Users (username, password, full_name, email, phone, role_id)
    SELECT 'admin',
           '$2a$12$hNAUBiPGi4bOFsIl5kLxuuFfpQ3Xs9Rg.n1RHrb5KVGSdB6dHgBy',
           N'Chủ Trọ Quản Trị',
           'admin@qlphongtro.vn',
           '0909000001',
           role_id
    FROM Roles WHERE role_name = 'ADMIN';
    PRINT '>>> Da them tai khoan admin (mat khau: 123456)';
END

IF NOT EXISTS (SELECT 1 FROM Users WHERE username = 'user01')
BEGIN
    INSERT INTO Users (username, password, full_name, email, phone, role_id)
    SELECT 'user01',
           '$2a$12$hNAUBiPGi4bOFsIl5kLxuuFfpQ3Xs9Rg.n1RHrb5KVGSdB6dHgBy',
           N'Nguyễn Văn An (Người thuê)',
           'an.nguyen@example.com',
           '0909000002',
           role_id
    FROM Roles WHERE role_name = 'USER';
    PRINT '>>> Da them tai khoan user01 (mat khau: 123456)';
END
GO
