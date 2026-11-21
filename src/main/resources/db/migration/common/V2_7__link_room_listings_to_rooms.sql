-- ============================================================================
-- V2_7__link_room_listings_to_rooms.sql
-- Liên kết bảng bài đăng room_listings với phòng vật lý rooms qua room_id
-- Cú pháp ANSI SQL tương thích cả SQL Server và H2
-- ============================================================================

-- 1. Thêm cột room_id (NULLABLE, chưa ép NOT NULL)
ALTER TABLE room_listings ADD room_id INT NULL;

-- 2. Thêm khóa ngoại trỏ sang rooms(room_id)
ALTER TABLE room_listings ADD CONSTRAINT fk_room_listings_rooms 
    FOREIGN KEY (room_id) REFERENCES rooms (room_id);

-- 3. Backfill room_id theo room_code CHỈ khi khớp duy nhất 1 dòng trong rooms
-- Tránh lỗi khi có nhiều tòa nhà trùng mã phòng (COUNT(*) > 1) hoặc chưa có phòng (COUNT(*) = 0)
UPDATE room_listings
SET room_id = (
    SELECT r.room_id 
    FROM rooms r 
    WHERE r.room_code = room_listings.room_code
)
WHERE (
    SELECT COUNT(*) 
    FROM rooms r 
    WHERE r.room_code = room_listings.room_code
) = 1;
