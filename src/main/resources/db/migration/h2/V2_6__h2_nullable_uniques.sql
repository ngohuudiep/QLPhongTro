-- ============================================================================
-- V2_6__h2_nullable_uniques.sql
-- Áp dụng riêng cho H2 Database (kiểm thử môi trường in-memory):
-- H2 hỗ trợ chuẩn ANSI SQL (cho phép nhiều giá trị NULL trong ràng buộc UNIQUE)
-- ============================================================================

ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);
ALTER TABLE users ADD CONSTRAINT uk_users_phone UNIQUE (phone);
ALTER TABLE tenants ADD CONSTRAINT uk_tenants_user_id UNIQUE (user_id);
