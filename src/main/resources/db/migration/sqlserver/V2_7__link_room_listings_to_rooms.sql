-- ============================================================================
-- V2_7__link_room_listings_to_rooms.sql (SQL Server)
-- Liên kết bảng bài đăng room_listings với phòng vật lý rooms qua room_id
-- ============================================================================

-- 1. Thêm cột room_id (NULLABLE, chưa ép NOT NULL)
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.room_listings') AND name = 'room_id')
BEGIN
    ALTER TABLE dbo.room_listings ADD room_id INT NULL;
END;

-- 2. Thêm khóa ngoại trỏ sang rooms(room_id)
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_room_listings_rooms')
BEGIN
    ALTER TABLE dbo.room_listings ADD CONSTRAINT fk_room_listings_rooms 
        FOREIGN KEY (room_id) REFERENCES dbo.rooms (room_id);
END;

-- 3. Backfill room_id theo room_code CHỈ khi khớp duy nhất 1 dòng trong rooms
-- Dùng dynamic SQL để tránh lỗi phân giải tên cột (Invalid column name) lúc biên dịch batch của SQL Server
EXEC sp_executesql N'
UPDATE dbo.room_listings
SET room_id = (
    SELECT r.room_id 
    FROM dbo.rooms r 
    WHERE r.room_code = dbo.room_listings.room_code
)
WHERE (
    SELECT COUNT(*) 
    FROM dbo.rooms r 
    WHERE r.room_code = dbo.room_listings.room_code
) = 1;
';
