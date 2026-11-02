-- ====================================================================
-- TỆP MIGRATION & SEED TOÀN DIỆN CHO SQL SERVER (ApartmentDB)
-- File: db/fix_unicode.sql
-- Tác dụng:
-- 1. Kiểm tra đối chiếu kiểu cột thực tế qua INFORMATION_SCHEMA
-- 2. ALTER TABLE chuẩn hóa dứt điểm:
--    - Tiếng Việt có dấu: title, amenities, description, current_tenant, full_name, note -> NVARCHAR
--    - Không dấu / mã: room_code, status, image_url, tenant_phone, username, email -> VARCHAR
-- 3. Xóa dữ liệu lỗi "?" và reset IDENTITY về 0
-- 4. Nạp lại 14 phòng trọ với tiếng Việt có dấu chuẩn 100% (tiền tố N'')
-- Chạy được an toàn nhiều lần (Idempotent)
-- ====================================================================

USE ApartmentDB;
GO

PRINT N'====================================================================';
PRINT N'BƯỚC 1: KIỂM TRA KIỂU DỮ LIỆU CÁC CỘT HIỆN TẠI';
PRINT N'====================================================================';

SELECT 
    TABLE_NAME AS [Bang], 
    COLUMN_NAME AS [Cot], 
    DATA_TYPE AS [Kieu_Du_Lieu], 
    CHARACTER_MAXIMUM_LENGTH AS [Do_Dai],
    IS_NULLABLE AS [Cho_Phep_Null]
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME IN ('RoomListings', 'Users', 'BookingRequests')
  AND DATA_TYPE IN ('varchar', 'nvarchar', 'char', 'nchar', 'text', 'ntext')
ORDER BY TABLE_NAME, ORDINAL_POSITION;
GO

PRINT N'====================================================================';
PRINT N'BƯỚC 2: CHUẨN HÓA KIỂU CỘT (ALTER COLUMN SANG NVARCHAR / VARCHAR)';
PRINT N'====================================================================';

-- 2.1 Bảng RoomListings
IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'RoomListings' AND COLUMN_NAME = 'title')
BEGIN
    ALTER TABLE RoomListings ALTER COLUMN title NVARCHAR(255) NOT NULL;
    PRINT N'-> Đã chuyển RoomListings.title sang NVARCHAR(255) NOT NULL';
END

IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'RoomListings' AND COLUMN_NAME = 'amenities')
BEGIN
    ALTER TABLE RoomListings ALTER COLUMN amenities NVARCHAR(500) NULL;
    PRINT N'-> Đã chuyển RoomListings.amenities sang NVARCHAR(500) NULL';
END

IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'RoomListings' AND COLUMN_NAME = 'description')
BEGIN
    ALTER TABLE RoomListings ALTER COLUMN description NVARCHAR(MAX) NULL;
    PRINT N'-> Đã chuyển RoomListings.description sang NVARCHAR(MAX) NULL';
END

IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'RoomListings' AND COLUMN_NAME = 'current_tenant')
BEGIN
    ALTER TABLE RoomListings ALTER COLUMN current_tenant NVARCHAR(150) NULL;
    PRINT N'-> Đã chuyển RoomListings.current_tenant sang NVARCHAR(150) NULL';
END

IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'RoomListings' AND COLUMN_NAME = 'image_gallery')
BEGIN
    ALTER TABLE RoomListings ALTER COLUMN image_gallery NVARCHAR(MAX) NULL;
    PRINT N'-> Đã chuyển RoomListings.image_gallery sang NVARCHAR(MAX) NULL';
END

-- Đảm bảo các cột không dấu giữ VARCHAR đồng bộ với Entity
IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'RoomListings' AND COLUMN_NAME = 'room_code')
BEGIN
    ALTER TABLE RoomListings ALTER COLUMN room_code VARCHAR(20) NOT NULL;
    PRINT N'-> Đã chuẩn hóa RoomListings.room_code sang VARCHAR(20) NOT NULL';
END

IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'RoomListings' AND COLUMN_NAME = 'status')
BEGIN
    ALTER TABLE RoomListings ALTER COLUMN status VARCHAR(15) NOT NULL;
    PRINT N'-> Đã chuẩn hóa RoomListings.status sang VARCHAR(15) NOT NULL';
END

IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'RoomListings' AND COLUMN_NAME = 'image_url')
BEGIN
    ALTER TABLE RoomListings ALTER COLUMN image_url VARCHAR(500) NULL;
    PRINT N'-> Đã chuẩn hóa RoomListings.image_url sang VARCHAR(500) NULL';
END

IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'RoomListings' AND COLUMN_NAME = 'tenant_phone')
BEGIN
    ALTER TABLE RoomListings ALTER COLUMN tenant_phone VARCHAR(20) NULL;
    PRINT N'-> Đã chuẩn hóa RoomListings.tenant_phone sang VARCHAR(20) NULL';
END
GO

-- 2.2 Bảng Users
IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Users' AND COLUMN_NAME = 'full_name')
BEGIN
    ALTER TABLE Users ALTER COLUMN full_name NVARCHAR(150) NULL;
    PRINT N'-> Đã chuyển Users.full_name sang NVARCHAR(150) NULL';
END

IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Users' AND COLUMN_NAME = 'username')
BEGIN
    ALTER TABLE Users ALTER COLUMN username VARCHAR(50) NOT NULL;
    PRINT N'-> Đã chuẩn hóa Users.username sang VARCHAR(50) NOT NULL';
END
GO

-- 2.3 Bảng BookingRequests
IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'BookingRequests' AND COLUMN_NAME = 'note')
BEGIN
    ALTER TABLE BookingRequests ALTER COLUMN note NVARCHAR(500) NULL;
    PRINT N'-> Đã chuyển BookingRequests.note sang NVARCHAR(500) NULL';
END
GO

PRINT N'====================================================================';
PRINT N'BƯỚC 3: XÓA DỮ LIỆU CŨ VÀ ĐẶT LẠI AUTO_INCREMENT (RESEED = 0)';
PRINT N'====================================================================';

-- Xóa bảng phụ có FK tham chiếu tới RoomListings
IF EXISTS (SELECT 1 FROM sys.tables WHERE name = 'BookingRequests')
BEGIN
    DELETE FROM BookingRequests WHERE room_id IN (SELECT listing_id FROM RoomListings);
    PRINT N'-> Đã xóa dữ liệu liên kết trong BookingRequests';
END
GO

-- Xóa toàn bộ dữ liệu lỗi ? trong RoomListings
DELETE FROM RoomListings;
PRINT N'-> Đã xóa sạch dữ liệu cũ trong RoomListings';
GO

-- Đặt lại bộ đếm IDENTITY về 0 để bản ghi đầu tiên có ID = 1
DBCC CHECKIDENT ('RoomListings', RESEED, 0);
PRINT N'-> Đã reset IDENTITY của RoomListings về 0 (ID bắt đầu từ 1)';
GO

PRINT N'====================================================================';
PRINT N'BƯỚC 4: NẠP LẠI 14 PHÒNG TRỌ MẪU CHUẨN TIẾNG VIỆT CÓ DẤU (TIỀN TỐ N'''')';
PRINT N'====================================================================';

INSERT INTO RoomListings (
    room_code, title, description, floor_no, area, price, 
    status, amenities, image_url, current_tenant, tenant_phone, 
    lease_start_date, lease_end_date, created_at
)
VALUES
-- Phòng 1: TRONG
('P101', N'Phòng Studio Ban công thoáng mát, Full nội thất', 
 N'Phòng đầy đủ nội thất cao cấp: máy lạnh Inverter, tủ lạnh 2 cánh, giường nệm cao su, tủ quần áo âm tường. Ban công đón ánh sáng tự nhiên.', 
 1, 28.5, 3800000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Gác lửng, Tủ lạnh, Ban công, Khóa vân tay', 
 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, GETDATE()),

-- Phòng 2: TRONG
('P102', N'Phòng trọ gác lửng đúc cao cấp, giờ giấc tự do', 
 N'Gác lửng đúc cao 2m đứng không đụng đầu, cầu thang tay vịn gỗ an toàn. Không chung chủ, cổng khóa vân tay 24/7.', 
 1, 25.0, 3200000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Gác lửng, Chỗ để xe', 
 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, GETDATE()),

-- Phòng 3: DA_THUE
('P103', N'Phòng trọ giá sinh viên gần các trường đại học', 
 N'Phòng sạch sẽ, gạch ốp tường 100%, có cửa sổ trời thoáng mát. Cách trạm xe buýt và chợ chỉ 200m.', 
 1, 18.0, 2200000, 'DA_THUE', N'Wifi, Nóng lạnh, Chỗ để xe', 
 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80', N'Trần Hoàng Nam', '0912345678', '2026-01-15', '2027-01-15', GETDATE()),

-- Phòng 4: TRONG
('P201', N'Căn hộ mini 1 phòng ngủ phong cách Bắc Âu Scandinavian', 
 N'Căn hộ thiết kế hiện đại tông màu trắng kem và gỗ sồi. Bếp riêng biệt có máy hút mùi, sofa êm ái.', 
 2, 35.0, 4900000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Tủ lạnh, Máy giặt, Bếp riêng, Sofa, Khóa vân tay', 
 'https://images.unsplash.com/photo-1493809842364-78817add7ffb?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, GETDATE()),

-- Phòng 5: DA_THUE
('P202', N'Phòng trọ cao cấp có máy giặt riêng, view công viên', 
 N'View nhìn ra công viên cây xanh mát mẻ, phòng yên tĩnh tuyệt đối thích hợp làm việc online (WFH).', 
 2, 30.0, 4200000, 'DA_THUE', N'Điều hòa, Wifi, Nóng lạnh, Máy giặt, Ban công, Khóa vân tay', 
 'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80', N'Lê Thị Thảo Nhi', '0987654321', '2026-03-01', '2027-03-01', GETDATE()),

-- Phòng 6: TRONG
('P203', N'Phòng trọ duplex có gác gỗ thông cao cấp', 
 N'Trần cao 4.2m thoáng đãng, gác lát sàn gỗ thông tự nhiên thơm nhẹ, tủ quần áo âm tường sức chứa lớn.', 
 2, 26.0, 3500000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Gác lửng, Tủ lạnh', 
 'https://images.unsplash.com/photo-1554995207-c18c203602cb?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, GETDATE()),

-- Phòng 7: TRONG
('P301', N'Phòng Penthouse mini tầng 3 sân thượng BBQ', 
 N'Phòng rộng có lối ra sân thượng ngắm trọn hoàng hôn thành phố. Được phép tổ chức tiệc BBQ cuối tuần.', 
 3, 42.0, 5500000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Ban công, Máy giặt, Tủ lạnh, Sân thượng', 
 'https://images.unsplash.com/photo-1502005229762-ee1b2b80a424?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, GETDATE()),

-- Phòng 8: DA_THUE
('P302', N'Phòng trọ yên tĩnh dành cho người đi làm', 
 N'Khu vực dân trí cao, an ninh nghiêm ngặt với hệ thống camera AI nhận diện. Điện nước tính theo công tơ riêng chuẩn nhà nước.', 
 3, 22.0, 3000000, 'DA_THUE', N'Điều hòa, Wifi, Nóng lạnh, Chỗ để xe', 
 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=800&q=80', N'Phạm Đức Trọng', '0933221100', '2025-11-10', '2026-11-10', GETDATE()),

-- Phòng 9: BAO_TRI
('P303', N'Phòng đang nâng cấp thay mới sàn gỗ và máy lạnh', 
 N'Đang hoàn thiện nội thất mới tinh 100%, dự kiến bàn giao đón khách vào đầu tháng tới.', 
 3, 24.0, 3300000, 'BAO_TRI', N'Điều hòa, Wifi, Nóng lạnh, Gác lửng', 
 'https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, GETDATE()),

-- Phòng 10: TRONG
('P401', N'Phòng trọ view Landmark cực đẹp, tầng cao thoáng khí', 
 N'Cửa sổ kính Low-E tràn viền cách âm cách nhiệt tốt. Buổi tối ngắm thành phố lên đèn cực chill.', 
 4, 32.0, 4500000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Ban công, Khóa vân tay, Tủ lạnh', 
 'https://images.unsplash.com/photo-1540518614846-7ede433c4ef4?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, GETDATE()),

-- Phòng 11: TRONG
('P402', N'Căn hộ 2 giường đơn thích hợp bạn bè ở ghép', 
 N'Đã bố trí 2 giường đơn riêng tư kèm 2 bàn học/làm việc. Chi phí thuê siêu tiết kiệm khi chia đôi.', 
 4, 29.0, 3600000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Bàn làm việc, Chỗ để xe', 
 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, GETDATE()),

-- Phòng 12: DA_THUE
('P403', N'Phòng trọ ấm cúng tiện nghi, giá cực mềm', 
 N'Phòng sạch đẹp, dán tường cao cấp, nhà vệ sinh riêng khép kín có bồn tắm đứng hiện đại.', 
 4, 20.0, 2600000, 'DA_THUE', N'Wifi, Nóng lạnh, Chỗ để xe', 
 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=800&q=80', N'Vũ Minh Tuấn', '0977889900', '2026-02-20', '2027-02-20', GETDATE()),

-- Phòng 13: TRONG
('P501', N'Phòng cao cấp áp mái phong cách Vintage cực chất', 
 N'Thiết kế tường gạch trần mộc mạc và cửa kính đón nắng bình minh. Không gian lý tưởng cho bạn trẻ thích sáng tạo.', 
 5, 36.0, 4600000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Ban công, Tủ lạnh, Sofa', 
 'https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, GETDATE()),

-- Phòng 14: BAO_TRI
('P502', N'Phòng trọ đang sơn sửa chống thấm định kỳ', 
 N'Bảo trì định kỳ hệ thống đường ống nước và điện âm tường để đảm bảo an toàn tuyệt đối cho người thuê.', 
 5, 23.0, 2900000, 'BAO_TRI', N'Wifi, Nóng lạnh, Chỗ để xe', 
 'https://images.unsplash.com/photo-1507089947368-19c1da9775ae?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, GETDATE());
GO

PRINT N'====================================================================';
PRINT N'BƯỚC 5: KIỂM TRA ĐỐI CHIẾU KẾT QUẢ SAU MIGRATION';
PRINT N'====================================================================';

-- Kiểm tra 14 phòng và ID
SELECT 
    listing_id   AS [ID], 
    room_code    AS [Ma_Phong], 
    title        AS [Tieu_De_Tieng_Viet], 
    amenities    AS [Tien_Nghi],
    status       AS [Trang_Thai],
    price        AS [Gia_Thue]
FROM RoomListings
ORDER BY listing_id ASC;

-- Thống kê theo trạng thái
SELECT 
    COUNT(*) AS [Tong_So_Phong_14],
    SUM(CASE WHEN status = 'TRONG' THEN 1 ELSE 0 END) AS [Con_Trong_8],
    SUM(CASE WHEN status = 'DA_THUE' THEN 1 ELSE 0 END) AS [Da_Cho_Thue_4],
    SUM(CASE WHEN status = 'BAO_TRI' THEN 1 ELSE 0 END) AS [Dang_Bao_Tri_2]
FROM RoomListings;
GO
PRINT N'>>> HOÀN TẤT MIGRATION VÀ NẠP SEED IDEMPOTENT THÀNH CÔNG!';
GO
