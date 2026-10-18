-- ====================================================================
-- Script tạo thêm bảng Roles, Users, RoomListings cho hệ thống
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
        user_id    INT IDENTITY(1,1) PRIMARY KEY,
        username   VARCHAR(50)  NOT NULL UNIQUE,
        password   VARCHAR(255) NOT NULL,         -- BCrypt hash
        full_name  NVARCHAR(150),
        role_id    INT NOT NULL,
        CONSTRAINT FK_Users_Roles FOREIGN KEY (role_id) REFERENCES Roles(role_id)
    );
    PRINT '>>> Da tao bang Users';
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
        area                DECIMAL(8, 2),
        price               DECIMAL(14, 2) NOT NULL,
        status              VARCHAR(15)    NOT NULL DEFAULT 'TRONG',  -- TRONG | DA_THUE | BAO_TRI
        image_url           VARCHAR(500),
        created_by_user_id  INT,
        CONSTRAINT FK_RoomListings_Users FOREIGN KEY (created_by_user_id) REFERENCES Users(user_id),
        CONSTRAINT CK_Status CHECK (status IN ('TRONG', 'DA_THUE', 'BAO_TRI'))
    );
    PRINT '>>> Da tao bang RoomListings';
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
-- Mật khẩu mặc định = "123456" đã mã hóa BCrypt strength=12
-- ================================================
IF NOT EXISTS (SELECT 1 FROM Users WHERE username = 'admin')
BEGIN
    INSERT INTO Users (username, password, full_name, role_id)
    SELECT 'admin',
           '$2a$12$hNAUBiPGi4bOFsIl5kLxuuFfpQ3Xs9Rg.n1RHrb5KVGSdB6dHgBy',
           N'Quản trị viên',
           role_id
    FROM Roles WHERE role_name = 'ADMIN';
    PRINT '>>> Da them tai khoan admin (password: 123456)';
END

IF NOT EXISTS (SELECT 1 FROM Users WHERE username = 'staff01')
BEGIN
    INSERT INTO Users (username, password, full_name, role_id)
    SELECT 'staff01',
           '$2a$12$hNAUBiPGi4bOFsIl5kLxuuFfpQ3Xs9Rg.n1RHrb5KVGSdB6dHgBy',
           N'Nhân viên 01',
           role_id
    FROM Roles WHERE role_name = 'STAFF';
    PRINT '>>> Da them tai khoan staff01 (password: 123456)';
END

IF NOT EXISTS (SELECT 1 FROM Users WHERE username = 'user01')
BEGIN
    INSERT INTO Users (username, password, full_name, role_id)
    SELECT 'user01',
           '$2a$12$hNAUBiPGi4bOFsIl5kLxuuFfpQ3Xs9Rg.n1RHrb5KVGSdB6dHgBy',
           N'Khách thuê 01',
           role_id
    FROM Roles WHERE role_name = 'USER';
    PRINT '>>> Da them tai khoan user01 (password: 123456)';
END
GO

-- ================================================
-- 6. Dữ liệu mẫu - RoomListings
-- ================================================
IF NOT EXISTS (SELECT 1 FROM RoomListings WHERE room_code = 'PHONG-01')
BEGIN
    DECLARE @adminId INT = (SELECT user_id FROM Users WHERE username = 'admin');

    INSERT INTO RoomListings (room_code, title, description, area, price, status, image_url, created_by_user_id)
    VALUES
    ('PHONG-01', N'Phòng trọ gần ĐH Bách Khoa, full nội thất', N'Phòng rộng rãi, thoáng mát, có điều hòa, tủ lạnh, máy giặt. Gần chợ, siêu thị.', 25.00, 3500000, 'TRONG', NULL, @adminId),
    ('PHONG-02', N'Phòng trọ trung tâm Q1, view đẹp', N'Phòng cao cấp, an ninh tốt, có bảo vệ 24/7. Gần các tuyến xe buýt.', 30.00, 5000000, 'TRONG', NULL, @adminId),
    ('PHONG-03', N'Phòng mini studio tiện nghi', N'Phòng nhỏ gọn đầy đủ tiện nghi, phù hợp sinh viên. Miễn phí wifi.', 18.00, 2800000, 'DA_THUE', NULL, @adminId),
    ('PHONG-04', N'Phòng trọ yên tĩnh, có gác lửng', N'Phòng gác lửng rộng, yên tĩnh, phù hợp cặp đôi. Có chỗ để xe miễn phí.', 35.00, 4200000, 'TRONG', NULL, @adminId),
    ('PHONG-05', N'Phòng đang bảo trì - sắp cho thuê', N'Phòng đang được sơn sửa lại, dự kiến cho thuê trong 2 tuần tới.', 22.00, 3000000, 'BAO_TRI', NULL, @adminId);

    PRINT '>>> Da them du lieu mau RoomListings';
END
GO

PRINT '=================================================';
PRINT 'HOAN TAT! Da tao xong Roles, Users, RoomListings.';
PRINT 'Tai khoan mac dinh (password: 123456):';
PRINT '  - admin   (ADMIN)';
PRINT '  - staff01 (STAFF)';
PRINT '  - user01  (USER)';
PRINT '=================================================';
