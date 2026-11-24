-- ============================================================================
-- V2_4__alter_room_listings_add_columns.sql (SQL Server)
-- Bổ sung các cột mở rộng cho bảng room_listings
-- Có kiểm tra an toàn để tương thích với cơ sở dữ liệu đã có sẵn một phần cột
-- ============================================================================

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.room_listings') AND name = 'floor_no')
BEGIN
    ALTER TABLE dbo.room_listings ADD floor_no INT NULL DEFAULT 1;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.room_listings') AND name = 'amenities')
BEGIN
    ALTER TABLE dbo.room_listings ADD amenities NVARCHAR(500) NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.room_listings') AND name = 'image_gallery')
BEGIN
    ALTER TABLE dbo.room_listings ADD image_gallery NVARCHAR(MAX) NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.room_listings') AND name = 'current_tenant')
BEGIN
    ALTER TABLE dbo.room_listings ADD current_tenant NVARCHAR(150) NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.room_listings') AND name = 'tenant_phone')
BEGIN
    ALTER TABLE dbo.room_listings ADD tenant_phone VARCHAR(20) NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.room_listings') AND name = 'lease_start_date')
BEGIN
    ALTER TABLE dbo.room_listings ADD lease_start_date DATE NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.room_listings') AND name = 'lease_end_date')
BEGIN
    ALTER TABLE dbo.room_listings ADD lease_end_date DATE NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.room_listings') AND name = 'created_at')
BEGIN
    ALTER TABLE dbo.room_listings ADD created_at DATETIME2 NULL;
END;
