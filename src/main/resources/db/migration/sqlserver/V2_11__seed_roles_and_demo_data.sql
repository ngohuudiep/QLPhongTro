-- ====================================================================
-- V2_11: Seed roles, 3 tài khoản demo và 14 phòng trọ mẫu
-- Đảm bảo tính lặp lại (idempotent), an toàn khi chạy trên DB mới hoặc DB hiện tại
-- ====================================================================

SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

-- 1. SEED ROLES
IF NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'ADMIN') INSERT INTO roles (role_name) VALUES ('ADMIN');
IF NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'STAFF') INSERT INTO roles (role_name) VALUES ('STAFF');
IF NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'USER') INSERT INTO roles (role_name) VALUES ('USER');
IF NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'LANDLORD') INSERT INTO roles (role_name) VALUES ('LANDLORD');
IF NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'TENANT') INSERT INTO roles (role_name) VALUES ('TENANT');

-- 2. SEED 3 DEMO ACCOUNTS
-- Mật khẩu hash BCrypt (cost 12):
-- admin: Admin@123456
-- chutro: Landlord@123456
-- nguoithue: Tenant@123456
IF NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin')
BEGIN
    INSERT INTO users (username, password, full_name, role_id, email, phone, account_status, account_enabled, enabled, registered_at, approved_at)
    SELECT 'admin', '$2a$12$IYgVwriio5rdYGC3ZxwCgO0YWd5.sF1o6rxzD74bcANi80ei6TRLu', N'Quản trị viên hệ thống',
           role_id, 'admin@qlphongtro.vn', '0901000001', 'APPROVED', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM roles WHERE role_name = 'ADMIN';
END

IF NOT EXISTS (SELECT 1 FROM users WHERE username = 'chutro')
BEGIN
    INSERT INTO users (username, password, full_name, role_id, email, phone, contact_address, account_status, account_enabled, enabled, registered_at, approved_at)
    SELECT 'chutro', '$2a$12$nziwln/9yIL20XaEaD.wSuvmhhaoxHlKfynL..rLCiQqyKwAVVsce', N'Chủ trọ Nguyễn Văn Nam',
           role_id, 'chutro@qlphongtro.vn', '0902000002', N'123 Đường Số 1, Phường Bến Nghé, Quận 1, TP.HCM', 'APPROVED', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM roles WHERE role_name = 'LANDLORD';
END

IF NOT EXISTS (SELECT 1 FROM users WHERE username = 'nguoithue')
BEGIN
    INSERT INTO users (username, password, full_name, role_id, email, phone, account_status, account_enabled, enabled, registered_at, approved_at)
    SELECT 'nguoithue', '$2a$12$XzffzGK0ct3FZ1FwHQMk3eC/4m.XE1NwXJd9nzVMuE/vF0Z/EL1Ja', N'Người thuê Lê Thị Hoa',
           role_id, 'nguoithue@qlphongtro.vn', '0903000003', 'APPROVED', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM roles WHERE role_name = 'TENANT';
END

-- 3. SEED DỮ LIỆU KHU TRỌ VÀ PHÒNG QUẢN LÝ CHO CHỦ TRỌ
IF NOT EXISTS (SELECT 1 FROM buildings WHERE building_code = 'TN-HM01')
BEGIN
    INSERT INTO buildings (building_code, building_name, address, city, total_floors, description, contact_phone, owner_user_id)
    SELECT 'TN-HM01', N'Nhà trọ Hoa Mai', N'123 Đường Số 1, Phường Bến Nghé, Quận 1', N'Hồ Chí Minh', 5, N'Tòa nhà trọ cao cấp trung tâm thành phố', '0902000002', user_id
    FROM users WHERE username = 'chutro';
END

IF NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'P101')
BEGIN
    INSERT INTO rooms (building_id, room_code, floor_no, max_occupants, price_override, status)
    SELECT b.building_id, 'P101', 1, 2, 3800000, 'TRONG'
    FROM buildings b WHERE b.building_code = 'TN-HM01';
END

IF NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'P103')
BEGIN
    INSERT INTO rooms (building_id, room_code, floor_no, max_occupants, price_override, status)
    SELECT b.building_id, 'P103', 1, 2, 2200000, 'DA_THUE'
    FROM buildings b WHERE b.building_code = 'TN-HM01';
END

-- 4. SEED DỊCH VỤ CƠ BẢN
IF NOT EXISTS (SELECT 1 FROM services WHERE service_code = 'DIEN')
    INSERT INTO services (service_code, service_name, unit, calc_type) VALUES ('DIEN', N'Điện', N'kWh', 'METER');
IF NOT EXISTS (SELECT 1 FROM services WHERE service_code = 'NUOC')
    INSERT INTO services (service_code, service_name, unit, calc_type) VALUES ('NUOC', N'Nước', N'm3', 'METER');
IF NOT EXISTS (SELECT 1 FROM services WHERE service_code = 'NET')
    INSERT INTO services (service_code, service_name, unit, calc_type) VALUES ('NET', N'Internet', N'tháng', 'FIXED');

-- 5. SEED 14 BÀI ĐĂNG PHÒNG TRỌ CÔNG KHAI
-- Xóa bài đăng tạm thời nếu có từ trước
DELETE FROM room_listings WHERE room_code = 'VN_DEMO_01';

IF NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P101')
    INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, created_at)
    VALUES ('P101', N'Phòng Studio Ban công thoáng mát, Full nội thất',
            N'Phòng đầy đủ nội thất cao cấp: máy lạnh Inverter, tủ lạnh 2 cánh, giường nệm cao su, tủ quần áo âm tường. Ban công đón ánh sáng tự nhiên.',
            1, 28.5, 3800000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Gác lửng, Tủ lạnh, Ban công, Khóa vân tay',
            'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80', CURRENT_TIMESTAMP);

IF NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P102')
    INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, created_at)
    VALUES ('P102', N'Phòng trọ gác lửng đúc cao cấp, giờ giấc tự do',
            N'Gác lửng đúc cao 2m đứng không đụng đầu, cầu thang tay vịn gỗ an toàn. Không chung chủ, cổng khóa vân tay 24/7.',
            1, 25.0, 3200000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Gác lửng, Chỗ để xe',
            'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80', CURRENT_TIMESTAMP);

IF NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P103')
    INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, current_tenant, tenant_phone, lease_start_date, lease_end_date, created_at)
    VALUES ('P103', N'Phòng trọ giá sinh viên gần các trường đại học',
            N'Phòng sạch sẽ, gạch ốp tường 100%, có cửa sổ trời thoáng mát. Cách trạm xe buýt và chợ chỉ 200m.',
            1, 18.0, 2200000, 'DA_THUE', N'Wifi, Nóng lạnh, Chỗ để xe',
            'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80',
            N'Trần Hoàng Nam', '0912345678', '2026-01-15', '2027-01-15', CURRENT_TIMESTAMP);

IF NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P201')
    INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, created_at)
    VALUES ('P201', N'Căn hộ mini 1 phòng ngủ phong cách Bắc Âu Scandinavian',
            N'Căn hộ thiết kế hiện đại tông màu trắng kem và gỗ sồi. Bếp riêng biệt có máy hút mùi, sofa êm ái.',
            2, 35.0, 4900000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Tủ lạnh, Máy giặt, Bếp riêng, Sofa, Khóa vân tay',
            'https://images.unsplash.com/photo-1493809842364-78817add7ffb?auto=format&fit=crop&w=800&q=80', CURRENT_TIMESTAMP);

IF NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P202')
    INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, current_tenant, tenant_phone, lease_start_date, lease_end_date, created_at)
    VALUES ('P202', N'Phòng trọ cao cấp có máy giặt riêng, view công viên',
            N'View nhìn ra công viên cây xanh mát mẻ, phòng yên tĩnh tuyệt đối thích hợp làm việc online (WFH).',
            2, 30.0, 4200000, 'DA_THUE', N'Điều hòa, Wifi, Nóng lạnh, Máy giặt, Ban công, Khóa vân tay',
            'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80',
            N'Lê Thị Thảo Nhi', '0987654321', '2026-03-01', '2027-03-01', CURRENT_TIMESTAMP);

IF NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P203')
    INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, created_at)
    VALUES ('P203', N'Phòng trọ duplex có gác gỗ thông cao cấp',
            N'Trần cao 4.2m thoáng đãng, gác lát sàn gỗ thông tự nhiên thơm nhẹ, tủ quần áo âm tường sức chứa lớn.',
            2, 26.0, 3500000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Gác lửng, Tủ lạnh',
            'https://images.unsplash.com/photo-1554995207-c18c203602cb?auto=format&fit=crop&w=800&q=80', CURRENT_TIMESTAMP);

IF NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P301')
    INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, created_at)
    VALUES ('P301', N'Phòng Penthouse mini tầng 3 sân thượng BBQ',
            N'Phòng rộng có lối ra sân thượng ngắm trọn hoàng hôn thành phố. Được phép tổ chức tiệc BBQ cuối tuần.',
            3, 42.0, 5500000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Ban công, Máy giặt, Tủ lạnh, Sân thượng',
            'https://images.unsplash.com/photo-1502005229762-ee1b2b80a424?auto=format&fit=crop&w=800&q=80', CURRENT_TIMESTAMP);

IF NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P302')
    INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, current_tenant, tenant_phone, lease_start_date, lease_end_date, created_at)
    VALUES ('P302', N'Phòng trọ yên tĩnh dành cho người đi làm',
            N'Khu vực dân trí cao, an ninh nghiêm ngặt với hệ thống camera AI nhận diện. Điện nước tính theo công tơ riêng chuẩn nhà nước.',
            3, 22.0, 3000000, 'DA_THUE', N'Điều hòa, Wifi, Nóng lạnh, Chỗ để xe',
            'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=800&q=80',
            N'Phạm Đức Trọng', '0933221100', '2025-11-10', '2026-11-10', CURRENT_TIMESTAMP);

IF NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P303')
    INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, created_at)
    VALUES ('P303', N'Phòng đang nâng cấp thay mới sàn gỗ và máy lạnh',
            N'Đang hoàn thiện nội thất mới tinh 100%, dự kiến bàn giao đón khách vào đầu tháng tới.',
            3, 24.0, 3300000, 'BAO_TRI', N'Điều hòa, Wifi, Nóng lạnh, Gác lửng',
            'https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?auto=format&fit=crop&w=800&q=80', CURRENT_TIMESTAMP);

IF NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P401')
    INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, created_at)
    VALUES ('P401', N'Phòng trọ view Landmark cực đẹp, tầng cao thoáng khí',
            N'Cửa sổ kính Low-E tràn viền cách âm cách nhiệt tốt. Buổi tối ngắm thành phố lên đèn cực chill.',
            4, 32.0, 4500000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Ban công, Khóa vân tay, Tủ lạnh',
            'https://images.unsplash.com/photo-1540518614846-7ede433c4ef4?auto=format&fit=crop&w=800&q=80', CURRENT_TIMESTAMP);

IF NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P402')
    INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, created_at)
    VALUES ('P402', N'Căn hộ 2 giường đơn thích hợp bạn bè ở ghép',
            N'Đã bố trí 2 giường đơn riêng tư kèm 2 bàn học/làm việc. Chi phí thuê siêu tiết kiệm khi chia đôi.',
            4, 29.0, 3600000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Bàn làm việc, Chỗ để xe',
            'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=800&q=80', CURRENT_TIMESTAMP);

IF NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P403')
    INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, current_tenant, tenant_phone, lease_start_date, lease_end_date, created_at)
    VALUES ('P403', N'Phòng trọ ấm cúng tiện nghi, giá cực mềm',
            N'Phòng sạch đẹp, dán tường cao cấp, nhà vệ sinh riêng khép kín có bồn tắm đứng hiện đại.',
            4, 20.0, 2600000, 'DA_THUE', N'Wifi, Nóng lạnh, Chỗ để xe',
            'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=800&q=80',
            N'Vũ Minh Tuấn', '0977889900', '2026-02-20', '2027-02-20', CURRENT_TIMESTAMP);

IF NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P501')
    INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, created_at)
    VALUES ('P501', N'Phòng cao cấp áp mái phong cách Vintage cực chất',
            N'Thiết kế tường gạch trần mộc mạc và cửa kính đón nắng bình minh. Không gian lý tưởng cho bạn trẻ thích sáng tạo.',
            5, 36.0, 4600000, 'TRONG', N'Điều hòa, Wifi, Nóng lạnh, Ban công, Tủ lạnh, Sofa',
            'https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=800&q=80', CURRENT_TIMESTAMP);

IF NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P502')
    INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, created_at)
    VALUES ('P502', N'Phòng trọ đang sơn sửa chống thấm định kỳ',
            N'Bảo trì định kỳ hệ thống đường ống nước và điện âm tường để đảm bảo an toàn tuyệt đối cho người thuê.',
            5, 23.0, 2900000, 'BAO_TRI', N'Wifi, Nóng lạnh, Chỗ để xe',
            'https://images.unsplash.com/photo-1507089947368-19c1da9775ae?auto=format&fit=crop&w=800&q=80', CURRENT_TIMESTAMP);

-- 6. ĐỒNG BỘ 1:1 MANAGED ROOM ID CHO BÀI ĐĂNG CÓ PHÒNG TƯƠNG ỨNG
UPDATE rl
SET rl.managed_room_id = r.room_id
FROM room_listings rl
JOIN rooms r ON r.room_code = rl.room_code
WHERE rl.managed_room_id IS NULL;
