-- ============================================================================
-- V2_6__sqlserver_filtered_uniques_and_fk_cleanup.sql
-- Áp dụng riêng cho Microsoft SQL Server:
-- 1. Tạo Filtered Unique Index cho các cột NULL (email, phone, user_id)
-- 2. Gỡ bỏ khóa ngoại từ Rooms sang RoomTypes và chuyển rooms.room_type_id sang NULL
-- 3. Dọn dẹp các khóa ngoại trùng lặp (Hibernate auto-generated hash FKs)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. TẠO FILTERED UNIQUE INDEX (WHERE ... IS NOT NULL)
-- ----------------------------------------------------------------------------
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'uq_users_email' AND object_id = OBJECT_ID('users'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX uq_users_email 
    ON users(email) 
    WHERE email IS NOT NULL;
END;

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'uq_users_phone' AND object_id = OBJECT_ID('users'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX uq_users_phone 
    ON users(phone) 
    WHERE phone IS NOT NULL;
END;

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'uq_tenants_user_id' AND object_id = OBJECT_ID('tenants'))
BEGIN
    CREATE UNIQUE NONCLUSTERED INDEX uq_tenants_user_id 
    ON tenants(user_id) 
    WHERE user_id IS NOT NULL;
END;

-- ----------------------------------------------------------------------------
-- 2. GỠ KHÓA NGOẠI TỪ Rooms SANG RoomTypes & CHUYỂN room_type_id SANG NULLABLE
-- ----------------------------------------------------------------------------
IF EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_Rooms_RoomTypes')
BEGIN
    ALTER TABLE Rooms DROP CONSTRAINT FK_Rooms_RoomTypes;
END;

-- Kiểm tra an toàn thêm bất kỳ FK nào khác từ Rooms sang RoomTypes
DECLARE @fk_name_rt NVARCHAR(200);
SELECT @fk_name_rt = name 
FROM sys.foreign_keys 
WHERE parent_object_id = OBJECT_ID('Rooms') 
  AND referenced_object_id = OBJECT_ID('RoomTypes');

IF @fk_name_rt IS NOT NULL
BEGIN
    EXEC('ALTER TABLE Rooms DROP CONSTRAINT ' + @fk_name_rt);
END;

-- Chuyển cột rooms.room_type_id sang NULLABLE
IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Rooms') AND name = 'room_type_id')
BEGIN
    ALTER TABLE Rooms ALTER COLUMN room_type_id INT NULL;
END;

-- ----------------------------------------------------------------------------
-- 3. DỌN DẸP CÁC KHÓA NGOẠI TRÙNG LẶP DO HIBERNATE TỰ SINH
-- (Giữ lại khóa ngoại chuẩn có tiền tố FK_..., xóa khóa ngoại hash ngẫu nhiên)
-- ----------------------------------------------------------------------------

-- Assets (room_id -> Rooms): giữ FK_Assets_Rooms, xóa FKnetab8erpxdgdle27ywjpblp1
IF EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FKnetab8erpxdgdle27ywjpblp1')
    ALTER TABLE Assets DROP CONSTRAINT FKnetab8erpxdgdle27ywjpblp1;

-- Contract_Tenants (contract_id -> Contracts): giữ FK_ContractTenants_Contracts, xóa FKdy8564lqd2ug89wqcbar2y661
IF EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FKdy8564lqd2ug89wqcbar2y661')
    ALTER TABLE Contract_Tenants DROP CONSTRAINT FKdy8564lqd2ug89wqcbar2y661;

-- Contract_Tenants (tenant_id -> Tenants): giữ FK_ContractTenants_Tenants, xóa FK5ueb6lgjgny7c11f7d51os92v
IF EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK5ueb6lgjgny7c11f7d51os92v')
    ALTER TABLE Contract_Tenants DROP CONSTRAINT FK5ueb6lgjgny7c11f7d51os92v;

-- Contracts (room_id -> Rooms): giữ FK_Contracts_Rooms, xóa FKju1b0xobla9t8oexrb8lpi8jq
IF EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FKju1b0xobla9t8oexrb8lpi8jq')
    ALTER TABLE Contracts DROP CONSTRAINT FKju1b0xobla9t8oexrb8lpi8jq;

-- Contracts (representative_id -> Tenants): giữ FK_Contracts_Tenants, xóa FKqfubtpv291wk82ldbb7ngepji
IF EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FKqfubtpv291wk82ldbb7ngepji')
    ALTER TABLE Contracts DROP CONSTRAINT FKqfubtpv291wk82ldbb7ngepji;

-- Incidents (room_id -> Rooms): giữ FK_Incidents_Rooms, xóa FKknlvote972dy3dl2r2ffble5u
IF EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FKknlvote972dy3dl2r2ffble5u')
    ALTER TABLE Incidents DROP CONSTRAINT FKknlvote972dy3dl2r2ffble5u;

-- Invoices (contract_id -> Contracts): giữ FK_Invoices_Contracts, xóa FKeads7q9fktwtsgdwmp1x16eqc
IF EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FKeads7q9fktwtsgdwmp1x16eqc')
    ALTER TABLE Invoices DROP CONSTRAINT FKeads7q9fktwtsgdwmp1x16eqc;

-- Payments (invoice_id -> Invoices): giữ FK_Payments_Invoices, xóa FKrbqec6be74wab8iifh8g3i50i
IF EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FKrbqec6be74wab8iifh8g3i50i')
    ALTER TABLE Payments DROP CONSTRAINT FKrbqec6be74wab8iifh8g3i50i;

-- Room_Services (room_id -> Rooms): giữ FK_RoomServices_Rooms, xóa FKewq1euu8r5i0c2f1ejfout7ty
IF EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FKewq1euu8r5i0c2f1ejfout7ty')
    ALTER TABLE Room_Services DROP CONSTRAINT FKewq1euu8r5i0c2f1ejfout7ty;

-- Room_Services (service_id -> Services): giữ FK_RoomServices_Services, xóa FKrvoqlh9yqrup1v41ejfevv6po
IF EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FKrvoqlh9yqrup1v41ejfevv6po')
    ALTER TABLE Room_Services DROP CONSTRAINT FKrvoqlh9yqrup1v41ejfevv6po;

-- Rooms (building_id -> Buildings): giữ FK_Rooms_Buildings, xóa FKojgn0sxhkfxd7pmmojnem9r4q
IF EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FKojgn0sxhkfxd7pmmojnem9r4q')
    ALTER TABLE Rooms DROP CONSTRAINT FKojgn0sxhkfxd7pmmojnem9r4q;

-- Users (role_id -> Roles): giữ FK_Users_Roles, xóa FKp56c1712k691lhsyewcssf40f
IF EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FKp56c1712k691lhsyewcssf40f')
    ALTER TABLE Users DROP CONSTRAINT FKp56c1712k691lhsyewcssf40f;
