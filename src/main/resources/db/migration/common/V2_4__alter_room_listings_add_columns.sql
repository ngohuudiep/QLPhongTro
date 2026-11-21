-- ============================================================================
-- V2_4__alter_room_listings_add_columns.sql
-- Bổ sung các cột mở rộng cho bảng room_listings (thông tin phòng, tiện nghi, hợp đồng)
-- Cú pháp ANSI SQL tương thích cả SQL Server và H2
-- ============================================================================

ALTER TABLE room_listings ADD floor_no INT NULL DEFAULT 1;
ALTER TABLE room_listings ADD amenities NVARCHAR(500) NULL;
ALTER TABLE room_listings ADD image_gallery NVARCHAR(MAX) NULL;
ALTER TABLE room_listings ADD current_tenant NVARCHAR(150) NULL;
ALTER TABLE room_listings ADD tenant_phone VARCHAR(20) NULL;
ALTER TABLE room_listings ADD lease_start_date DATE NULL;
ALTER TABLE room_listings ADD lease_end_date DATE NULL;
ALTER TABLE room_listings ADD created_at DATETIME2 NULL;
