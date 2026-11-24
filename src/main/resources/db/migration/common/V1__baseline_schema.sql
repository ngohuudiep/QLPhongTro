-- ============================================================================
-- V1__baseline_schema.sql
-- Baseline Schema Snapshot (Trạng thái hiện tại của cơ sở dữ liệu thật)
-- Cú pháp chuẩn hóa, tương thích cả SQL Server và H2 (MODE=MSSQLServer)
-- Clean baseline: rooms.room_type_id để NULL, không FK sang RoomTypes, không FK trùng
-- ============================================================================

-- 1. Bảng roles (Vai trò hệ thống)
CREATE TABLE roles (
    role_id INT IDENTITY(1,1) NOT NULL,
    role_name VARCHAR(20) NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (role_id),
    CONSTRAINT uk_roles_role_name UNIQUE (role_name)
);

-- 2. Bảng users (Tài khoản người dùng)
CREATE TABLE users (
    user_id INT IDENTITY(1,1) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name NVARCHAR(150) NULL,
    role_id INT NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT fk_users_roles FOREIGN KEY (role_id) REFERENCES roles (role_id)
);

-- 3. Bảng buildings (Tòa nhà / Cơ sở phòng trọ)
CREATE TABLE buildings (
    building_id INT IDENTITY(1,1) NOT NULL,
    building_code VARCHAR(20) NOT NULL,
    building_name VARCHAR(150) NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NULL,
    total_floors SMALLINT NULL,
    CONSTRAINT pk_buildings PRIMARY KEY (building_id)
);

-- 4. Bảng rooms (Danh mục phòng trọ)
-- room_type_id để NULL, không tạo FK sang RoomTypes cũ
CREATE TABLE rooms (
    room_id INT IDENTITY(1,1) NOT NULL,
    building_id INT NOT NULL,
    room_type_id INT NULL,
    room_code VARCHAR(20) NOT NULL,
    floor_no SMALLINT NULL,
    max_occupants SMALLINT NULL,
    price_override DECIMAL(14,2) NULL,
    status VARCHAR(15) NULL,
    CONSTRAINT pk_rooms PRIMARY KEY (room_id),
    CONSTRAINT fk_rooms_buildings FOREIGN KEY (building_id) REFERENCES buildings (building_id)
);

-- 5. Bảng tenants (Khách thuê)
CREATE TABLE tenants (
    tenant_id INT IDENTITY(1,1) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    id_number VARCHAR(20) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    hometown VARCHAR(255) NULL,
    date_of_birth DATE NULL,
    email VARCHAR(150) NULL,
    CONSTRAINT pk_tenants PRIMARY KEY (tenant_id),
    CONSTRAINT uk_tenants_id_number UNIQUE (id_number)
);

-- 6. Bảng contracts (Hợp đồng thuê)
CREATE TABLE contracts (
    contract_id INT IDENTITY(1,1) NOT NULL,
    contract_code VARCHAR(30) NOT NULL,
    room_id INT NOT NULL,
    representative_id INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    deposit_amount DECIMAL(14,2) NULL,
    billing_cycle_months SMALLINT NULL,
    status VARCHAR(15) NULL,
    CONSTRAINT pk_contracts PRIMARY KEY (contract_id),
    CONSTRAINT fk_contracts_rooms FOREIGN KEY (room_id) REFERENCES rooms (room_id),
    CONSTRAINT fk_contracts_tenants FOREIGN KEY (representative_id) REFERENCES tenants (tenant_id)
);

-- 7. Bảng contract_tenants (Bảng quan hệ hợp đồng - khách thuê cùng ở)
CREATE TABLE contract_tenants (
    contract_id INT NOT NULL,
    tenant_id INT NOT NULL,
    move_in_date DATE NOT NULL,
    move_out_date DATE NULL,
    CONSTRAINT pk_contract_tenants PRIMARY KEY (contract_id, tenant_id),
    CONSTRAINT fk_contract_tenants_contracts FOREIGN KEY (contract_id) REFERENCES contracts (contract_id),
    CONSTRAINT fk_contract_tenants_tenants FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id)
);

-- 8. Bảng invoices (Hóa đơn tiền phòng & dịch vụ)
CREATE TABLE invoices (
    invoice_id INT IDENTITY(1,1) NOT NULL,
    invoice_code VARCHAR(30) NOT NULL,
    contract_id INT NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    due_date DATE NOT NULL,
    total_amount DECIMAL(14,2) NULL,
    status VARCHAR(30) NULL,
    CONSTRAINT pk_invoices PRIMARY KEY (invoice_id),
    CONSTRAINT fk_invoices_contracts FOREIGN KEY (contract_id) REFERENCES contracts (contract_id)
);

-- 9. Bảng payments (Thanh toán hóa đơn)
CREATE TABLE payments (
    payment_id INT IDENTITY(1,1) NOT NULL,
    invoice_id INT NOT NULL,
    amount DECIMAL(14,2) NOT NULL,
    paid_at DATETIME NULL,
    method VARCHAR(15) NOT NULL,
    reference_no VARCHAR(50) NULL,
    CONSTRAINT pk_payments PRIMARY KEY (payment_id),
    CONSTRAINT fk_payments_invoices FOREIGN KEY (invoice_id) REFERENCES invoices (invoice_id)
);

-- 10. Bảng services (Danh mục dịch vụ)
CREATE TABLE services (
    service_id INT IDENTITY(1,1) NOT NULL,
    service_code VARCHAR(20) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    calc_type VARCHAR(20) NOT NULL,
    CONSTRAINT pk_services PRIMARY KEY (service_id)
);

-- 11. Bảng room_services (Đơn giá dịch vụ áp dụng theo phòng)
CREATE TABLE room_services (
    price_id INT IDENTITY(1,1) NOT NULL,
    service_id INT NOT NULL,
    room_id INT NOT NULL,
    unit_price DECIMAL(14,2) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    CONSTRAINT pk_room_services PRIMARY KEY (price_id),
    CONSTRAINT fk_room_services_services FOREIGN KEY (service_id) REFERENCES services (service_id),
    CONSTRAINT fk_room_services_rooms FOREIGN KEY (room_id) REFERENCES rooms (room_id)
);

-- 12. Bảng assets (Tài sản / trang thiết bị phòng)
CREATE TABLE assets (
    room_asset_id INT IDENTITY(1,1) NOT NULL,
    room_id INT NOT NULL,
    asset_name VARCHAR(150) NOT NULL,
    quantity SMALLINT NULL DEFAULT 1,
    condition_status VARCHAR(50) NULL,
    purchase_date DATE NULL,
    CONSTRAINT pk_assets PRIMARY KEY (room_asset_id),
    CONSTRAINT fk_assets_rooms FOREIGN KEY (room_id) REFERENCES rooms (room_id)
);

-- 13. Bảng incidents (Sự cố / yêu cầu sửa chữa)
CREATE TABLE incidents (
    incident_id INT IDENTITY(1,1) NOT NULL,
    room_id INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    priority VARCHAR(10) NULL,
    status VARCHAR(15) NULL,
    repair_cost DECIMAL(14,2) NULL,
    reported_at DATETIME NULL,
    resolved_at DATETIME NULL,
    CONSTRAINT pk_incidents PRIMARY KEY (incident_id),
    CONSTRAINT fk_incidents_rooms FOREIGN KEY (room_id) REFERENCES rooms (room_id)
);

-- 14. Bảng meter_readings (Ghi nhận chỉ số điện / nước)
CREATE TABLE meter_readings (
    reading_id INT IDENTITY(1,1) NOT NULL,
    room_id INT NOT NULL,
    billing_month DATE NOT NULL,
    start_index DECIMAL(12,2) NOT NULL,
    end_index DECIMAL(12,2) NOT NULL,
    consumption DECIMAL(12,2) NOT NULL,
    read_date DATE NOT NULL,
    CONSTRAINT pk_meter_readings PRIMARY KEY (reading_id),
    CONSTRAINT fk_meter_readings_rooms FOREIGN KEY (room_id) REFERENCES rooms (room_id)
);

-- 15. Bảng room_listings (Thông tin bài đăng phòng trọ của ứng dụng)
CREATE TABLE room_listings (
    listing_id INT IDENTITY(1,1) NOT NULL,
    room_code VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description NVARCHAR(MAX) NULL,
    area DECIMAL(8,2) NULL,
    price DECIMAL(14,2) NOT NULL,
    status VARCHAR(15) NOT NULL,
    image_url VARCHAR(500) NULL,
    created_by_user_id INT NULL,
    CONSTRAINT pk_room_listings PRIMARY KEY (listing_id),
    CONSTRAINT uk_room_listings_room_code UNIQUE (room_code),
    CONSTRAINT fk_room_listings_users FOREIGN KEY (created_by_user_id) REFERENCES users (user_id)
);

-- 16. Bảng booking_requests (Yêu cầu đặt phòng từ khách thuê)
CREATE TABLE booking_requests (
    request_id INT IDENTITY(1,1) NOT NULL,
    user_id INT NOT NULL,
    room_id INT NOT NULL,
    request_date DATETIME2 NOT NULL,
    status VARCHAR(15) NOT NULL,
    note VARCHAR(500) NULL,
    processed_by_user_id INT NULL,
    processed_date DATETIME2 NULL,
    CONSTRAINT pk_booking_requests PRIMARY KEY (request_id),
    CONSTRAINT fk_booking_requests_users FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_booking_requests_room_listings FOREIGN KEY (room_id) REFERENCES room_listings (listing_id),
    CONSTRAINT fk_booking_requests_processed_by FOREIGN KEY (processed_by_user_id) REFERENCES users (user_id)
);
