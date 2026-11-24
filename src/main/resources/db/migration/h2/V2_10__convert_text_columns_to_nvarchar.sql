-- ============================================================================
-- V2_10__convert_text_columns_to_nvarchar.sql (H2)
-- Chuyển các cột văn bản sang NVARCHAR cho tương thích H2
-- ============================================================================

ALTER TABLE assets ALTER COLUMN asset_name NVARCHAR(150) NOT NULL;
ALTER TABLE assets ALTER COLUMN condition_status NVARCHAR(50) NULL;

ALTER TABLE booking_requests ALTER COLUMN note NVARCHAR(500) NULL;

ALTER TABLE buildings ALTER COLUMN building_name NVARCHAR(150) NOT NULL;
ALTER TABLE buildings ALTER COLUMN address NVARCHAR(255) NOT NULL;
ALTER TABLE buildings ALTER COLUMN city NVARCHAR(100) NULL;

ALTER TABLE incidents ALTER COLUMN title NVARCHAR(200) NOT NULL;

ALTER TABLE payments ALTER COLUMN method NVARCHAR(50) NOT NULL;

ALTER TABLE room_listings ALTER COLUMN title NVARCHAR(255) NOT NULL;
ALTER TABLE room_listings ALTER COLUMN amenities NVARCHAR(500) NULL;

ALTER TABLE services ALTER COLUMN service_name NVARCHAR(100) NOT NULL;
ALTER TABLE services ALTER COLUMN unit NVARCHAR(20) NOT NULL;

ALTER TABLE tenants ALTER COLUMN full_name NVARCHAR(150) NOT NULL;
ALTER TABLE tenants ALTER COLUMN hometown NVARCHAR(255) NULL;
