-- ============================================================================
-- V2_5__add_business_uniques.sql
-- Thêm ràng buộc duy nhất cho mã tòa nhà (building_code) và mã dịch vụ (service_code)
-- Cú pháp ANSI SQL tương thích cả SQL Server và H2
-- ============================================================================

ALTER TABLE buildings ADD CONSTRAINT uk_buildings_building_code UNIQUE (building_code);
ALTER TABLE services ADD CONSTRAINT uk_services_service_code UNIQUE (service_code);
