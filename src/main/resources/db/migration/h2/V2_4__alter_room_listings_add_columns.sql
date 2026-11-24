-- ============================================================================
-- V2_4__alter_room_listings_add_columns.sql (H2)
-- Bổ sung các cột mở rộng cho bảng room_listings
-- ============================================================================

ALTER TABLE room_listings ADD IF NOT EXISTS floor_no INT NULL DEFAULT 1;
ALTER TABLE room_listings ADD IF NOT EXISTS amenities NVARCHAR(500) NULL;
ALTER TABLE room_listings ADD IF NOT EXISTS image_gallery NVARCHAR(MAX) NULL;
ALTER TABLE room_listings ADD IF NOT EXISTS current_tenant NVARCHAR(150) NULL;
ALTER TABLE room_listings ADD IF NOT EXISTS tenant_phone VARCHAR(20) NULL;
ALTER TABLE room_listings ADD IF NOT EXISTS lease_start_date DATE NULL;
ALTER TABLE room_listings ADD IF NOT EXISTS lease_end_date DATE NULL;
ALTER TABLE room_listings ADD IF NOT EXISTS created_at DATETIME2 NULL;
