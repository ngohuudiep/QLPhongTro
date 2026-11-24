-- ====================================================================
-- V2_11: Seed roles, 3 tài khoản demo và 14 phòng trọ mẫu (H2 dialect)
-- ====================================================================

-- 1. SEED ROLES
MERGE INTO roles (role_name) KEY(role_name) VALUES ('ADMIN');
MERGE INTO roles (role_name) KEY(role_name) VALUES ('STAFF');
MERGE INTO roles (role_name) KEY(role_name) VALUES ('USER');
MERGE INTO roles (role_name) KEY(role_name) VALUES ('LANDLORD');
MERGE INTO roles (role_name) KEY(role_name) VALUES ('TENANT');

-- 2. SEED 3 DEMO ACCOUNTS
INSERT INTO users (username, password, full_name, role_id, email, phone, account_status, account_enabled, enabled, registered_at, approved_at)
SELECT 'admin', '$2a$12$IYgVwriio5rdYGC3ZxwCgO0YWd5.sF1o6rxzD74bcANi80ei6TRLu', 'Quản trị viên hệ thống',
       role_id, 'admin@qlphongtro.vn', '0901000001', 'APPROVED', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles WHERE role_name = 'ADMIN'
AND NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

INSERT INTO users (username, password, full_name, role_id, email, phone, contact_address, account_status, account_enabled, enabled, registered_at, approved_at)
SELECT 'chutro', '$2a$12$nziwln/9yIL20XaEaD.wSuvmhhaoxHlKfynL..rLCiQqyKwAVVsce', 'Chủ trọ Nguyễn Văn Nam',
       role_id, 'chutro@qlphongtro.vn', '0902000002', '123 Đường Số 1, Phường Bến Nghé, Quận 1, TP.HCM', 'APPROVED', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles WHERE role_name = 'LANDLORD'
AND NOT EXISTS (SELECT 1 FROM users WHERE username = 'chutro');

INSERT INTO users (username, password, full_name, role_id, email, phone, account_status, account_enabled, enabled, registered_at, approved_at)
SELECT 'nguoithue', '$2a$12$XzffzGK0ct3FZ1FwHQMk3eC/4m.XE1NwXJd9nzVMuE/vF0Z/EL1Ja', 'Người thuê Lê Thị Hoa',
       role_id, 'nguoithue@qlphongtro.vn', '0903000003', 'APPROVED', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles WHERE role_name = 'TENANT'
AND NOT EXISTS (SELECT 1 FROM users WHERE username = 'nguoithue');

-- 3. SEED KHU TRỌ VÀ PHÒNG CHO CHỦ TRỌ
INSERT INTO buildings (building_code, building_name, address, city, total_floors, description, contact_phone, owner_user_id)
SELECT 'TN-HM01', 'Nhà trọ Hoa Mai', '123 Đường Số 1, Phường Bến Nghé, Quận 1', 'Hồ Chí Minh', 5, 'Tòa nhà trọ cao cấp trung tâm thành phố', '0902000002', user_id
FROM users WHERE username = 'chutro'
AND NOT EXISTS (SELECT 1 FROM buildings WHERE building_code = 'TN-HM01');

INSERT INTO rooms (building_id, room_code, floor_no, max_occupants, price_override, status)
SELECT b.building_id, 'P101', 1, 2, 3800000, 'TRONG'
FROM buildings b WHERE b.building_code = 'TN-HM01'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'P101');

INSERT INTO rooms (building_id, room_code, floor_no, max_occupants, price_override, status)
SELECT b.building_id, 'P103', 1, 2, 2200000, 'DA_THUE'
FROM buildings b WHERE b.building_code = 'TN-HM01'
AND NOT EXISTS (SELECT 1 FROM rooms WHERE room_code = 'P103');

-- 4. SEED DỊCH VỤ CƠ BẢN
MERGE INTO services (service_code, service_name, unit, calc_type) KEY(service_code) VALUES ('DIEN', 'Điện', 'kWh', 'METER');
MERGE INTO services (service_code, service_name, unit, calc_type) KEY(service_code) VALUES ('NUOC', 'Nước', 'm3', 'METER');
MERGE INTO services (service_code, service_name, unit, calc_type) KEY(service_code) VALUES ('NET', 'Internet', 'tháng', 'FIXED');

-- 5. SEED 14 BÀI ĐĂNG PHÒNG TRỌ CÔNG KHAI
DELETE FROM room_listings WHERE room_code = 'VN_DEMO_01';

INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, created_at)
SELECT 'P101', 'Phòng Studio Ban công thoáng mát, Full nội thất',
       'Phòng đầy đủ nội thất cao cấp: máy lạnh Inverter, tủ lạnh 2 cánh, giường nệm cao su, tủ quần áo âm tường. Ban công đón ánh sáng tự nhiên.',
       1, 28.5, 3800000, 'TRONG', 'Điều hòa, Wifi, Nóng lạnh, Gác lửng, Tủ lạnh, Ban công, Khóa vân tay',
       'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P101');

INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, created_at)
SELECT 'P102', 'Phòng trọ gác lửng đúc cao cấp, giờ giấc tự do',
       'Gác lửng đúc cao 2m đứng không đụng đầu, cầu thang tay vịn gỗ an toàn. Không chung chủ, cổng khóa vân tay 24/7.',
       1, 25.0, 3200000, 'TRONG', 'Điều hòa, Wifi, Nóng lạnh, Gác lửng, Chỗ để xe',
       'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P102');

INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, current_tenant, tenant_phone, lease_start_date, lease_end_date, created_at)
SELECT 'P103', 'Phòng trọ giá sinh viên gần các trường đại học',
       'Phòng sạch sẽ, gạch ốp tường 100%, có cửa sổ trời thoáng mát. Cách trạm xe buýt và chợ chỉ 200m.',
       1, 18.0, 2200000, 'DA_THUE', 'Wifi, Nóng lạnh, Chỗ để xe',
       'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80',
       'Trần Hoàng Nam', '0912345678', DATE '2026-01-15', DATE '2027-01-15', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P103');

INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, created_at)
SELECT 'P201', 'Căn hộ mini 1 phòng ngủ phong cách Bắc Âu Scandinavian',
       'Căn hộ thiết kế hiện đại tông màu trắng kem và gỗ sồi. Bếp riêng biệt có máy hút mùi, sofa êm ái.',
       2, 35.0, 4900000, 'TRONG', 'Điều hòa, Wifi, Nóng lạnh, Tủ lạnh, Máy giặt, Bếp riêng, Sofa, Khóa vân tay',
       'https://images.unsplash.com/photo-1493809842364-78817add7ffb?auto=format&fit=crop&w=800&q=80', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P201');

INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, current_tenant, tenant_phone, lease_start_date, lease_end_date, created_at)
SELECT 'P202', 'Phòng trọ cao cấp có máy giặt riêng, view công viên',
       'View nhìn ra công viên cây xanh mát mẻ, phòng yên tĩnh tuyệt đối thích hợp làm việc online (WFH).',
       2, 30.0, 4200000, 'DA_THUE', 'Điều hòa, Wifi, Nóng lạnh, Máy giặt, Ban công, Khóa vân tay',
       'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80',
       'Lê Thị Thảo Nhi', '0987654321', DATE '2026-03-01', DATE '2027-03-01', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P202');

INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, created_at)
SELECT 'P203', 'Phòng trọ duplex có gác gỗ thông cao cấp',
       'Trần cao 4.2m thoáng đãng, gác lát sàn gỗ thông tự nhiên thơm nhẹ, tủ quần áo âm tường sức chứa lớn.',
       2, 26.0, 3500000, 'TRONG', 'Điều hòa, Wifi, Nóng lạnh, Gác lửng, Tủ lạnh',
       'https://images.unsplash.com/photo-1554995207-c18c203602cb?auto=format&fit=crop&w=800&q=80', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P203');

INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, created_at)
SELECT 'P301', 'Phòng Penthouse mini tầng 3 sân thượng BBQ',
       'Phòng rộng có lối ra sân thượng ngắm trọn hoàng hôn thành phố. Được phép tổ chức tiệc BBQ cuối tuần.',
       3, 42.0, 5500000, 'TRONG', 'Điều hòa, Wifi, Nóng lạnh, Ban công, Máy giặt, Tủ lạnh, Sân thượng',
       'https://images.unsplash.com/photo-1502005229762-ee1b2b80a424?auto=format&fit=crop&w=800&q=80', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P301');

INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, current_tenant, tenant_phone, lease_start_date, lease_end_date, created_at)
SELECT 'P302', 'Phòng trọ yên tĩnh dành cho người đi làm',
       'Khu vực dân trí cao, an ninh nghiêm ngặt với hệ thống camera AI nhận diện. Điện nước tính theo công tơ riêng chuẩn nhà nước.',
       3, 22.0, 3000000, 'DA_THUE', 'Điều hòa, Wifi, Nóng lạnh, Chỗ để xe',
       'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=800&q=80',
       'Phạm Đức Trọng', '0933221100', DATE '2025-11-10', DATE '2026-11-10', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P302');

INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, created_at)
SELECT 'P303', 'Phòng đang nâng cấp thay mới sàn gỗ và máy lạnh',
       'Đang hoàn thiện nội thất mới tinh 100%, dự kiến bàn giao đón khách vào đầu tháng tới.',
       3, 24.0, 3300000, 'BAO_TRI', 'Điều hòa, Wifi, Nóng lạnh, Gác lửng',
       'https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?auto=format&fit=crop&w=800&q=80', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P303');

INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, created_at)
SELECT 'P401', 'Phòng trọ view Landmark cực đẹp, tầng cao thoáng khí',
       'Cửa sổ kính Low-E tràn viền cách âm cách nhiệt tốt. Buổi tối ngắm thành phố lên đèn cực chill.',
       4, 32.0, 4500000, 'TRONG', 'Điều hòa, Wifi, Nóng lạnh, Ban công, Khóa vân tay, Tủ lạnh',
       'https://images.unsplash.com/photo-1540518614846-7ede433c4ef4?auto=format&fit=crop&w=800&q=80', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P401');

INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, created_at)
SELECT 'P402', 'Căn hộ 2 giường đơn thích hợp bạn bè ở ghép',
       'Đã bố trí 2 giường đơn riêng tư kèm 2 bàn học/làm việc. Chi phí thuê siêu tiết kiệm khi chia đôi.',
       4, 29.0, 3600000, 'TRONG', 'Điều hòa, Wifi, Nóng lạnh, Bàn làm việc, Chỗ để xe',
       'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=800&q=80', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P402');

INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, current_tenant, tenant_phone, lease_start_date, lease_end_date, created_at)
SELECT 'P403', 'Phòng trọ ấm cúng tiện nghi, giá cực mềm',
       'Phòng sạch đẹp, dán tường cao cấp, nhà vệ sinh riêng khép kín có bồn tắm đứng hiện đại.',
       4, 20.0, 2600000, 'DA_THUE', 'Wifi, Nóng lạnh, Chỗ để xe',
       'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=800&q=80',
       'Vũ Minh Tuấn', '0977889900', DATE '2026-02-20', DATE '2027-02-20', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P403');

INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, created_at)
SELECT 'P501', 'Phòng cao cấp áp mái phong cách Vintage cực chất',
       'Thiết kế tường gạch trần mộc mạc và cửa kính đón nắng bình minh. Không gian lý tưởng cho bạn trẻ thích sáng tạo.',
       5, 36.0, 4600000, 'TRONG', 'Điều hòa, Wifi, Nóng lạnh, Ban công, Tủ lạnh, Sofa',
       'https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=800&q=80', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P501');

INSERT INTO room_listings (room_code, title, description, floor_no, area, price, status, amenities, image_url, created_at)
SELECT 'P502', 'Phòng trọ đang sơn sửa chống thấm định kỳ',
       'Bảo trì định kỳ hệ thống đường ống nước và điện âm tường để đảm bảo an toàn tuyệt đối cho người thuê.',
       5, 23.0, 2900000, 'BAO_TRI', 'Wifi, Nóng lạnh, Chỗ để xe',
       'https://images.unsplash.com/photo-1507089947368-19c1da9775ae?auto=format&fit=crop&w=800&q=80', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM room_listings WHERE room_code = 'P502');

-- 6. ĐỒNG BỘ 1:1 MANAGED ROOM ID
UPDATE room_listings
SET managed_room_id = (SELECT r.room_id FROM rooms r WHERE r.room_code = room_listings.room_code)
WHERE managed_room_id IS NULL AND EXISTS (SELECT 1 FROM rooms r WHERE r.room_code = room_listings.room_code);
