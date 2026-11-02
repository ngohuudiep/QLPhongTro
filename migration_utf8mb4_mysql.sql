-- ====================================================================
-- MIGRATION CHUYỂN TOÀN BỘ CƠ SỞ DỮ LIỆU SANG utf8mb4 CHO MYSQL / MARIADB
-- Database: ApartmentDB
-- ====================================================================

-- 1. Thiết lập charset cho phiên làm việc
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 2. Kiểm tra các cột và bảng chưa phải utf8mb4
SELECT 
    TABLE_NAME, 
    COLUMN_NAME, 
    CHARACTER_SET_NAME, 
    COLLATION_NAME 
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'ApartmentDB'
  AND CHARACTER_SET_NAME IS NOT NULL
  AND CHARACTER_SET_NAME != 'utf8mb4';

-- 3. Chuyển database sang utf8mb4
ALTER DATABASE ApartmentDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 4. Chuyển TẤT CẢ các bảng sang utf8mb4_unicode_ci
ALTER TABLE RoomListings CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE Users CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE Roles CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE BookingRequests CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Nếu có các bảng cũ
DROP PROCEDURE IF EXISTS AlterIfTableExists;
DELIMITER //
CREATE PROCEDURE AlterIfTableExists(IN tableName VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = 'ApartmentDB' AND table_name = tableName
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', tableName, ' CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL AlterIfTableExists('Rooms');
CALL AlterIfTableExists('Buildings');
CALL AlterIfTableExists('phong_tro');
CALL AlterIfTableExists('khu_tro');
CALL AlterIfTableExists('loai_phong');
DROP PROCEDURE IF EXISTS AlterIfTableExists;

-- 5. Xóa dữ liệu cũ bị dấu "?" và nạp lại 14 phòng chuẩn UTF-8
DELETE FROM BookingRequests WHERE room_id > 0;
DELETE FROM RoomListings WHERE listing_id > 0;
ALTER TABLE RoomListings AUTO_INCREMENT = 1;

INSERT INTO RoomListings (
    room_code, title, description, floor_no, area, price, 
    status, amenities, image_url, current_tenant, tenant_phone, 
    lease_start_date, lease_end_date, created_at
)
VALUES
('P101', 'Phòng Studio Ban công thoáng mát, Full nội thất', 
 'Phòng đầy đủ nội thất cao cấp: máy lạnh Inverter, tủ lạnh 2 cánh, giường nệm cao su, tủ quần áo âm tường. Ban công đón ánh sáng tự nhiên.', 
 1, 28.5, 3800000, 'TRONG', 'Điều hòa, Wifi, Nóng lạnh, Gác lửng, Tủ lạnh, Ban công, Khóa vân tay', 
 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, NOW()),

('P102', 'Phòng trọ gác lửng đúc cao cấp, giờ giấc tự do', 
 'Gác lửng đúc cao 2m đứng không đụng đầu, cầu thang tay vịn gỗ an toàn. Không chung chủ, cổng khóa vân tay 24/7.', 
 1, 25.0, 3200000, 'TRONG', 'Điều hòa, Wifi, Nóng lạnh, Gác lửng, Chỗ để xe', 
 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, NOW()),

('P103', 'Phòng trọ giá sinh viên gần các trường đại học', 
 'Phòng sạch sẽ, gạch ốp tường 100%, có cửa sổ trời thoáng mát. Cách trạm xe buýt và chợ chỉ 200m.', 
 1, 18.0, 2200000, 'DA_THUE', 'Wifi, Nóng lạnh, Chỗ để xe', 
 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80', 'Trần Hoàng Nam', '0912345678', '2026-01-15', '2027-01-15', NOW()),

('P201', 'Căn hộ mini 1 phòng ngủ phong cách Bắc Âu Scandinavian', 
 'Căn hộ thiết kế hiện đại tông màu trắng kem và gỗ sồi. Bếp riêng biệt có máy hút mùi, sofa êm ái.', 
 2, 35.0, 4900000, 'TRONG', 'Điều hòa, Wifi, Nóng lạnh, Tủ lạnh, Máy giặt, Bếp riêng, Sofa, Khóa vân tay', 
 'https://images.unsplash.com/photo-1493809842364-78817add7ffb?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, NOW()),

('P202', 'Phòng trọ cao cấp có máy giặt riêng, view công viên', 
 'View nhìn ra công viên cây xanh mát mẻ, phòng yên tĩnh tuyệt đối thích hợp làm việc online (WFH).', 
 2, 30.0, 4200000, 'DA_THUE', 'Điều hòa, Wifi, Nóng lạnh, Máy giặt, Ban công, Khóa vân tay', 
 'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80', 'Lê Thị Thảo Nhi', '0987654321', '2026-03-01', '2027-03-01', NOW()),

('P203', 'Phòng trọ duplex có gác gỗ thông cao cấp', 
 'Trần cao 4.2m thoáng đãng, gác lát sàn gỗ thông tự nhiên thơm nhẹ, tủ quần áo âm tường sức chứa lớn.', 
 2, 26.0, 3500000, 'TRONG', 'Điều hòa, Wifi, Nóng lạnh, Gác lửng, Tủ lạnh', 
 'https://images.unsplash.com/photo-1554995207-c18c203602cb?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, NOW()),

('P301', 'Phòng Penthouse mini tầng 3 sân thượng BBQ', 
 'Phòng rộng có lối ra sân thượng ngắm trọn hoàng hôn thành phố. Được phép tổ chức tiệc BBQ cuối tuần.', 
 3, 42.0, 5500000, 'TRONG', 'Điều hòa, Wifi, Nóng lạnh, Ban công, Máy giặt, Tủ lạnh, Sân thượng', 
 'https://images.unsplash.com/photo-1502005229762-ee1b2b80a424?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, NOW()),

('P302', 'Phòng trọ yên tĩnh dành cho người đi làm', 
 'Khu vực dân trí cao, an ninh nghiêm ngặt với hệ thống camera AI nhận diện. Điện nước tính theo công tơ riêng chuẩn nhà nước.', 
 3, 22.0, 3000000, 'DA_THUE', 'Điều hòa, Wifi, Nóng lạnh, Chỗ để xe', 
 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=800&q=80', 'Phạm Đức Trọng', '0933221100', '2025-11-10', '2026-11-10', NOW()),

('P303', 'Phòng đang nâng cấp thay mới sàn gỗ và máy lạnh', 
 'Đang hoàn thiện nội thất mới tinh 100%, dự kiến bàn giao đón khách vào đầu tháng tới.', 
 3, 24.0, 3300000, 'BAO_TRI', 'Điều hòa, Wifi, Nóng lạnh, Gác lửng', 
 'https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, NOW()),

('P401', 'Phòng trọ view Landmark cực đẹp, tầng cao thoáng khí', 
 'Cửa sổ kính Low-E tràn viền cách âm cách nhiệt tốt. Buổi tối ngắm thành phố lên đèn cực chill.', 
 4, 32.0, 4500000, 'TRONG', 'Điều hòa, Wifi, Nóng lạnh, Ban công, Khóa vân tay, Tủ lạnh', 
 'https://images.unsplash.com/photo-1540518614846-7ede433c4ef4?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, NOW()),

('P402', 'Căn hộ 2 giường đơn thích hợp bạn bè ở ghép', 
 'Đã bố trí 2 giường đơn riêng tư kèm 2 bàn học/làm việc. Chi phí thuê siêu tiết kiệm khi chia đôi.', 
 4, 29.0, 3600000, 'TRONG', 'Điều hòa, Wifi, Nóng lạnh, Bàn làm việc, Chỗ để xe', 
 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, NOW()),

('P403', 'Phòng trọ ấm cúng tiện nghi, giá cực mềm', 
 'Phòng sạch đẹp, dán tường cao cấp, nhà vệ sinh riêng khép kín có bồn tắm đứng hiện đại.', 
 4, 20.0, 2600000, 'DA_THUE', 'Wifi, Nóng lạnh, Chỗ để xe', 
 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=800&q=80', 'Vũ Minh Tuấn', '0977889900', '2026-02-20', '2027-02-20', NOW()),

('P501', 'Phòng cao cấp áp mái phong cách Vintage cực chất', 
 'Thiết kế tường gạch trần mộc mạc và cửa kính đón nắng bình minh. Không gian lý tưởng cho bạn trẻ thích sáng tạo.', 
 5, 36.0, 4600000, 'TRONG', 'Điều hòa, Wifi, Nóng lạnh, Ban công, Tủ lạnh, Sofa', 
 'https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, NOW()),

('P502', 'Phòng trọ đang sơn sửa chống thấm định kỳ', 
 'Bảo trì định kỳ hệ thống đường ống nước và điện âm tường để đảm bảo an toàn tuyệt đối cho người thuê.', 
 5, 23.0, 2900000, 'BAO_TRI', 'Wifi, Nóng lạnh, Chỗ để xe', 
 'https://images.unsplash.com/photo-1507089947368-19c1da9775ae?auto=format&fit=crop&w=800&q=80', NULL, NULL, NULL, NULL, NOW());

-- 6. Kiểm tra lại dữ liệu sau khi nạp
SELECT listing_id, room_code, title, amenities FROM RoomListings ORDER BY listing_id ASC;
