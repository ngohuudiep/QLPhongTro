-- ====================================================================
-- SCRIPT CHẨN ĐOÁN & SỬA LỖI DỮ LIỆU PHÒNG TRỌ (ApartmentDB)
-- Tác dụng:
-- 1. Kiểm tra ID thực tế hiện tại của các phòng
-- 2. Xóa sạch dữ liệu cũ và reset AUTO_INCREMENT về 0
-- 3. Nạp lại chuẩn xác 14 phòng mẫu với ID ổn định từ 1 đến 14
-- 4. Xác nhận thống kê khớp chuẩn: 8 Còn trống, 4 Đã thuê, 2 Bảo trì
-- Chạy script này trong SQL Server Management Studio (SSMS)
-- ====================================================================

USE ApartmentDB;
GO

-- ====================================================================
-- BƯỚC 1: KIỂM TRA DỮ LIỆU HIỆN TẠI (CHẨN ĐOÁN ID)
-- ====================================================================
PRINT N'=== 1. DANH SÁCH ID PHÒNG TRỌ HIỆN TẠI TRONG CƠ SỞ DỮ LIỆU ===';
SELECT 
    listing_id   AS [ID_Thuc_Te], 
    room_code    AS [Ma_Phong], 
    title        AS [Tieu_De], 
    status       AS [Trang_Thai],
    price        AS [Gia_Thue],
    floor_no     AS [Tang]
FROM RoomListings
ORDER BY listing_id ASC;
GO

-- ====================================================================
-- BƯỚC 2: XÓA DỮ LIỆU CŨ & RESET ĐẶT LẠI AUTO_INCREMENT (IDENTITY = 0)
-- ====================================================================
PRINT N'=== 2. ĐANG RESET BẢNG VÀ ĐẶT LẠI BỘ ĐẾM AUTO_INCREMENT ===';

-- Xóa liên kết booking nếu có để tránh lỗi Foreign Key
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'BookingRequests')
BEGIN
    DELETE FROM BookingRequests WHERE room_id IN (SELECT listing_id FROM RoomListings);
    PRINT N'>>> Đã dọn dẹp các yêu cầu đặt phòng cũ liên kết';
END
GO

-- Xóa dữ liệu phòng trọ cũ
DELETE FROM RoomListings;
GO

-- ĐẶT LẠI BỘ ĐẾM IDENTITY VỀ 0 ĐỂ DÒNG ĐẦU TIÊN CÓ ID = 1
DBCC CHECKIDENT ('RoomListings', RESEED, 0);
GO
PRINT N'>>> Đã reset bộ đếm IDENTITY về 0 thành công!';
GO

-- ====================================================================
-- BƯỚC 3: NẠP LẠI CHUẨN XÁC 14 PHÒNG TRỌ TIẾNG VIỆT UNICODE
-- Phân bổ: 8 Còn trống (TRONG), 4 Đã thuê (DA_THUE), 2 Bảo trì (BAO_TRI)
-- ====================================================================
PRINT N'=== 3. NẠP 14 PHÒNG TRỌ VỚI ID TỪ 1 ĐẾN 14 ===';

INSERT INTO RoomListings (
    room_code, title, description, floor_no, area, price, 
    status, amenities, image_url, current_tenant, tenant_phone, 
    lease_start_date, lease_end_date, created_at
)
VALUES
-- ID 1 (Tầng 1 - TRONG)
('P101', N'Phòng Studio Ban công thoáng mát, Full nội thất', 
 N'Phòng đầy đủ nội thất cao cấp: máy lạnh Inverter, tủ lạnh 2 cánh, giường nệm cao su, tủ quần áo âm tường. Ban công đón ánh sáng tự nhiên.', 
 1, 28.5, 3800000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Gác lửng, Tủ lạnh, Ban công, Khóa vân tay', 
 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, GETDATE()),

-- ID 2 (Tầng 1 - TRONG)
('P102', N'Phòng trọ gác lửng đúc cao cấp, giờ giấc tự do', 
 N'Gác lửng đúc cao 2m đứng không đụng đầu, cầu thang tay vịn gỗ an toàn. Không chung chủ, cổng khóa vân tay 24/7.', 
 1, 25.0, 3200000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Gác lửng, Chỗ để xe', 
 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, GETDATE()),

-- ID 3 (Tầng 1 - DA_THUE)
('P103', N'Phòng trọ giá sinh viên gần các trường đại học', 
 N'Phòng sạch sẽ, gạch ốp tường 100%, có cửa sổ trời thoáng mát. Cách trạm xe buýt và chợ chỉ 200m.', 
 1, 18.0, 2200000, 'DA_THUE', N'Wifi, Nóng lạnh, Chỗ để xe', 
 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80', N'Trần Hoàng Nam', '0912345678', '2026-01-15', '2027-01-15', GETDATE()),

-- ID 4 (Tầng 2 - TRONG)
('P201', N'Căn hộ mini 1 phòng ngủ phong cách Bắc Âu Scandinavian', 
 N'Căn hộ thiết kế hiện đại tông màu trắng kem và gỗ sồi. Bếp riêng biệt có máy hút mùi, sofa êm ái.', 
 2, 35.0, 4900000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Tủ lạnh, Máy giặt, Bếp riêng, Sofa, Khóa vân tay', 
 'https://images.unsplash.com/photo-1493809842364-78817add7ffb?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, GETDATE()),

-- ID 5 (Tầng 2 - DA_THUE)
('P202', N'Phòng trọ cao cấp có máy giặt riêng, view công viên', 
 N'View nhìn ra công viên cây xanh mát mẻ, phòng yên tĩnh tuyệt đối thích hợp làm việc online (WFH).', 
 2, 30.0, 4200000, 'DA_THUE', N'Điều hòa, Wifi, Nóng lạnh, Máy giặt, Ban công, Khóa vân tay', 
 'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80', N'Lê Thị Thảo Nhi', '0987654321', '2026-03-01', '2027-03-01', GETDATE()),

-- ID 6 (Tầng 2 - TRONG)
('P203', N'Phòng trọ duplex có gác gỗ thông cao cấp', 
 N'Trần cao 4.2m thoáng đãng, gác lát sàn gỗ thông tự nhiên thơm nhẹ, tủ quần áo âm tường sức chứa lớn.', 
 2, 26.0, 3500000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Gác lửng, Tủ lạnh', 
 'https://images.unsplash.com/photo-1554995207-c18c203602cb?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, GETDATE()),

-- ID 7 (Tầng 3 - TRONG)
('P301', N'Phòng Penthouse mini tầng 3 sân thượng BBQ', 
 N'Phòng rộng có lối ra sân thượng ngắm trọn hoàng hôn thành phố. Được phép tổ chức tiệc BBQ cuối tuần.', 
 3, 42.0, 5500000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Ban công, Máy giặt, Tủ lạnh, Sân thượng', 
 'https://images.unsplash.com/photo-1502005229762-ee1b2b80a424?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, GETDATE()),

-- ID 8 (Tầng 3 - DA_THUE)
('P302', N'Phòng trọ yên tĩnh dành cho người đi làm', 
 N'Khu vực dân trí cao, an ninh nghiêm ngặt với hệ thống camera AI nhận diện. Điện nước tính theo công tơ riêng chuẩn nhà nước.', 
 3, 22.0, 3000000, 'DA_THUE', N'Điều hòa, Wifi, Nóng lạnh, Chỗ để xe', 
 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=800&q=80', N'Phạm Đức Trọng', '0933221100', '2025-11-10', '2026-11-10', GETDATE()),

-- ID 9 (Tầng 3 - BAO_TRI)
('P303', N'Phòng đang nâng cấp thay mới sàn gỗ và máy lạnh', 
 N'Đang hoàn thiện nội thất mới tinh 100%, dự kiến bàn giao đón khách vào đầu tháng tới.', 
 3, 24.0, 3300000, 'BAO_TRI', N'Điều hòa, Wifi, Nóng lạnh, Gác lửng', 
 'https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, GETDATE()),

-- ID 10 (Tầng 4 - TRONG)
('P401', N'Phòng trọ view Landmark cực đẹp, tầng cao thoáng khí', 
 N'Cửa sổ kính Low-E tràn viền cách âm cách nhiệt tốt. Buổi tối ngắm thành phố lên đèn cực chill.', 
 4, 32.0, 4500000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Ban công, Khóa vân tay, Tủ lạnh', 
 'https://images.unsplash.com/photo-1540518614846-7ede433c4ef4?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, GETDATE()),

-- ID 11 (Tầng 4 - TRONG)
('P402', N'Căn hộ 2 giường đơn thích hợp bạn bè ở ghép', 
 N'Đã bố trí 2 giường đơn riêng tư kèm 2 bàn học/làm việc. Chi phí thuê siêu tiết kiệm khi chia đôi.', 
 4, 29.0, 3600000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Bàn làm việc, Chỗ để xe', 
 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, GETDATE()),

-- ID 12 (Tầng 4 - DA_THUE)
('P403', N'Phòng trọ ấm cúng tiện nghi, giá cực mềm', 
 N'Phòng sạch đẹp, dán tường cao cấp, nhà vệ sinh riêng khép kín có bồn tắm đứng hiện đại.', 
 4, 20.0, 2600000, 'DA_THUE', N'Wifi, Nóng lạnh, Chỗ để xe', 
 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=800&q=80', N'Vũ Minh Tuấn', '0977889900', '2026-02-20', '2027-02-20', GETDATE()),

-- ID 13 (Tầng 5 - TRONG)
('P501', N'Phòng cao cấp áp mái phong cách Vintage cực chất', 
 N'Thiết kế tường gạch trần mộc mạc và cửa kính đón nắng bình minh. Không gian lý tưởng cho bạn trẻ thích sáng tạo.', 
 5, 36.0, 4600000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Ban công, Tủ lạnh, Sofa', 
 'https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, GETDATE()),

-- ID 14 (Tầng 5 - BAO_TRI)
('P502', N'Phòng trọ đang sơn sửa chống thấm định kỳ', 
 N'Bảo trì định kỳ hệ thống đường ống nước và điện âm tường để đảm bảo an toàn tuyệt đối cho người thuê.', 
 5, 23.0, 2900000, 'BAO_TRI', N'Wifi, Nóng lạnh, Chỗ để xe', 
 'https://images.unsplash.com/photo-1507089947368-19c1da9775ae?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, GETDATE());
GO

-- ====================================================================
-- BƯỚC 4: KIỂM TRA LẠI KẾT QUẢ SAU KHI NẠP DỮ LIỆU
-- ====================================================================
PRINT N'=== 4. KẾT QUẢ SAU KHI SỬA VÀ NẠP LẠI (ID ĐƯỢC ĐÁNH SỐ TỪ 1 ĐẾN 14) ===';

SELECT 
    listing_id   AS [ID], 
    room_code    AS [Ma_Phong], 
    title        AS [Tieu_De], 
    status       AS [Trang_Thai],
    price        AS [Gia_Thue],
    floor_no     AS [Tang]
FROM RoomListings
ORDER BY listing_id ASC;
GO

PRINT N'=== THỐNG KÊ THEO TRẠNG THÁI ===';
SELECT 
    COUNT(*) AS [Tong_So_Phong],
    SUM(CASE WHEN status = 'TRONG' THEN 1 ELSE 0 END) AS [Con_Trong],
    SUM(CASE WHEN status = 'DA_THUE' THEN 1 ELSE 0 END) AS [Da_Cho_Thue],
    SUM(CASE WHEN status = 'BAO_TRI' THEN 1 ELSE 0 END) AS [Dang_Bao_Tri]
FROM RoomListings;
GO
