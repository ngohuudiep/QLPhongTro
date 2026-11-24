-- ============================================================================
-- V2_2__alter_tenants_and_incidents_add_fk.sql
-- Thêm khóa ngoại tenants.user_id và incidents.tenant_id
-- Cú pháp ANSI SQL tương thích cả SQL Server và H2
-- ============================================================================

ALTER TABLE tenants ADD user_id INT NULL;
ALTER TABLE tenants ADD CONSTRAINT fk_tenants_users FOREIGN KEY (user_id) REFERENCES users (user_id);

ALTER TABLE incidents ADD tenant_id INT NULL;
ALTER TABLE incidents ADD CONSTRAINT fk_incidents_tenants FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id);
