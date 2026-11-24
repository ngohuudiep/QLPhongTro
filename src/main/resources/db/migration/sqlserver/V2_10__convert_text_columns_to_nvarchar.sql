-- ============================================================================
-- V2_10__convert_text_columns_to_nvarchar.sql (SQL Server)
-- Chuyển toàn bộ các cột chứa chuỗi văn bản, tên, địa chỉ, ghi chú, tiêu đề sang NVARCHAR
-- để lưu trữ đầy đủ và chính xác ký tự tiếng Việt có dấu Unicode UTF-8
-- ============================================================================

ALTER TABLE dbo.assets ALTER COLUMN asset_name NVARCHAR(150) NOT NULL;
ALTER TABLE dbo.assets ALTER COLUMN condition_status NVARCHAR(50) NULL;

ALTER TABLE dbo.booking_requests ALTER COLUMN note NVARCHAR(500) NULL;

ALTER TABLE dbo.buildings ALTER COLUMN building_name NVARCHAR(150) NOT NULL;
ALTER TABLE dbo.buildings ALTER COLUMN address NVARCHAR(255) NOT NULL;
ALTER TABLE dbo.buildings ALTER COLUMN city NVARCHAR(100) NULL;

ALTER TABLE dbo.incidents ALTER COLUMN title NVARCHAR(200) NOT NULL;

ALTER TABLE dbo.payments ALTER COLUMN method NVARCHAR(50) NOT NULL;

ALTER TABLE dbo.room_listings ALTER COLUMN title NVARCHAR(255) NOT NULL;
ALTER TABLE dbo.room_listings ALTER COLUMN amenities NVARCHAR(500) NULL;

ALTER TABLE dbo.services ALTER COLUMN service_name NVARCHAR(100) NOT NULL;
ALTER TABLE dbo.services ALTER COLUMN unit NVARCHAR(20) NOT NULL;

ALTER TABLE dbo.tenants ALTER COLUMN full_name NVARCHAR(150) NOT NULL;
ALTER TABLE dbo.tenants ALTER COLUMN hometown NVARCHAR(255) NULL;
