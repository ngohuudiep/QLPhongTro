-- =============================================
-- Script tạo Database và 4 bảng danh mục cơ bản
-- Tên Database: ApartmentDB
-- =============================================

-- 1. Tạo Database ApartmentDB nếu chưa tồn tại
IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'ApartmentDB')
BEGIN
    CREATE DATABASE ApartmentDB;
END
GO

-- 2. Sử dụng Database ApartmentDB
USE ApartmentDB;
GO

-- 3. Tạo bảng RoomTypes
CREATE TABLE RoomTypes (
    room_type_id   INT IDENTITY(1,1) PRIMARY KEY,
    type_name      VARCHAR(200) NOT NULL,
    default_price  DECIMAL(14,2) NOT NULL,
    area_m2        DECIMAL(8,2) NULL,
    description    VARCHAR(255) NULL
);
GO

-- 4. Tạo bảng Buildings
CREATE TABLE Buildings (
    building_id    INT IDENTITY(1,1) PRIMARY KEY,
    building_code  VARCHAR(20) NOT NULL,
    building_name  VARCHAR(150) NOT NULL,
    address        VARCHAR(255) NOT NULL,
    city           VARCHAR(100) NULL,
    total_floors   SMALLINT NULL
);
GO

-- 5. Tạo bảng Services
CREATE TABLE Services (
    service_id     INT IDENTITY(1,1) PRIMARY KEY,
    service_code   VARCHAR(20) NOT NULL,
    service_name   VARCHAR(100) NOT NULL,
    unit           VARCHAR(20) NOT NULL,
    calc_type      VARCHAR(20) NOT NULL
);
GO

-- 6. Tạo bảng Tenants
CREATE TABLE Tenants (
    tenant_id      INT IDENTITY(1,1) PRIMARY KEY,
    full_name      VARCHAR(150) NOT NULL,
    id_number      VARCHAR(20) NOT NULL,
    phone          VARCHAR(20) NOT NULL,
    hometown       VARCHAR(255) NULL,
    date_of_birth  DATE NULL,
    email          VARCHAR(150) NULL
);
GO

-- 7. Tạo bảng Rooms (Phòng)
CREATE TABLE Rooms (
    room_id         INT IDENTITY(1,1) PRIMARY KEY,
    building_id     INT NOT NULL,
    room_type_id    INT NOT NULL,
    room_code       VARCHAR(20) NOT NULL,
    floor_no        SMALLINT NULL,
    max_occupants   SMALLINT NULL,
    price_override  DECIMAL(14,2) NULL,
    status          VARCHAR(15) NULL,

    -- Khóa ngoại (Foreign Keys)
    CONSTRAINT FK_Rooms_Buildings FOREIGN KEY (building_id) REFERENCES Buildings(building_id),
    CONSTRAINT FK_Rooms_RoomTypes FOREIGN KEY (room_type_id) REFERENCES RoomTypes(room_type_id)
);
GO

-- 8. Tạo bảng Room_Services (Dịch vụ theo phòng)
CREATE TABLE Room_Services (
    price_id        INT IDENTITY(1,1) PRIMARY KEY,
    service_id      INT NOT NULL,
    room_id         INT NOT NULL,
    unit_price      DECIMAL(14,2) NOT NULL,
    effective_from  DATE NOT NULL,
    effective_to    DATE NULL,

    -- Khóa ngoại (Foreign Keys)
    CONSTRAINT FK_RoomServices_Services FOREIGN KEY (service_id) REFERENCES Services(service_id),
    CONSTRAINT FK_RoomServices_Rooms FOREIGN KEY (room_id) REFERENCES Rooms(room_id)
);
GO

-- 9. Tạo bảng Contracts (Hợp đồng thuê phòng)
CREATE TABLE Contracts (
    contract_id           INT IDENTITY(1,1) PRIMARY KEY,
    contract_code         VARCHAR(30) NOT NULL,
    room_id               INT NOT NULL,
    representative_id     INT NOT NULL,
    start_date            DATE NOT NULL,
    end_date              DATE NOT NULL,
    deposit_amount        DECIMAL(14,2) NULL,
    billing_cycle_months  SMALLINT NULL,
    status                VARCHAR(15) NULL,

    -- Khóa ngoại (Foreign Keys)
    CONSTRAINT FK_Contracts_Rooms FOREIGN KEY (room_id) REFERENCES Rooms(room_id),
    CONSTRAINT FK_Contracts_Tenants FOREIGN KEY (representative_id) REFERENCES Tenants(tenant_id)
);
GO

-- 10. Tạo bảng Contract_Tenants (Danh sách khách theo hợp đồng)
CREATE TABLE Contract_Tenants (
    contract_id     INT NOT NULL,
    tenant_id       INT NOT NULL,
    move_in_date    DATE NOT NULL,
    move_out_date   DATE NULL,

    -- Khóa chính phức hợp (Composite Primary Key)
    CONSTRAINT PK_Contract_Tenants PRIMARY KEY (contract_id, tenant_id),

    -- Khóa ngoại (Foreign Keys)
    CONSTRAINT FK_ContractTenants_Contracts FOREIGN KEY (contract_id) REFERENCES Contracts(contract_id),
    CONSTRAINT FK_ContractTenants_Tenants FOREIGN KEY (tenant_id) REFERENCES Tenants(tenant_id)
);
GO

-- 11. Tạo bảng Invoices (Hóa đơn tiền phòng & dịch vụ)
CREATE TABLE Invoices (
    invoice_id    INT IDENTITY(1,1) PRIMARY KEY,
    invoice_code  VARCHAR(30) NOT NULL,
    contract_id   INT NOT NULL,
    period_start  DATE NOT NULL,
    period_end    DATE NOT NULL,
    due_date      DATE NOT NULL,
    total_amount  DECIMAL(14,2) NULL,
    status        VARCHAR(30) NULL,

    -- Khóa ngoại
    CONSTRAINT FK_Invoices_Contracts FOREIGN KEY (contract_id) REFERENCES Contracts(contract_id)
);
GO

-- 12. Tạo bảng Payments (Lịch sử thanh toán hóa đơn)
CREATE TABLE Payments (
    payment_id    INT IDENTITY(1,1) PRIMARY KEY,
    invoice_id    INT NOT NULL,
    amount        DECIMAL(14,2) NOT NULL,
    paid_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    method        VARCHAR(15) NOT NULL,
    reference_no  VARCHAR(50) NULL,

    -- Khóa ngoại
    CONSTRAINT FK_Payments_Invoices FOREIGN KEY (invoice_id) REFERENCES Invoices(invoice_id)
);
GO

-- 13. Tạo bảng Assets (Tài sản trang thiết bị trong phòng)
CREATE TABLE Assets (
    room_asset_id     INT IDENTITY(1,1) PRIMARY KEY,
    room_id           INT NOT NULL,
    asset_name        VARCHAR(150) NOT NULL,
    quantity          SMALLINT DEFAULT 1,
    condition_status  VARCHAR(50) NULL,
    purchase_date     DATE NULL,

    -- Khóa ngoại
    CONSTRAINT FK_Assets_Rooms FOREIGN KEY (room_id) REFERENCES Rooms(room_id)
);
GO

-- 14. Tạo bảng Incidents (Báo cáo sự cố phòng trọ)
CREATE TABLE Incidents (
    incident_id       INT IDENTITY(1,1) PRIMARY KEY,
    room_id           INT NOT NULL,
    title             VARCHAR(200) NOT NULL,
    priority          VARCHAR(10) NULL, -- High, Medium, Low
    status            VARCHAR(15) NULL, -- Tiếp nhận, Đang xử lý, Hoàn thành, Đã hủy
    repair_cost       DECIMAL(14,2) NULL,
    reported_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    resolved_at       DATETIME NULL,

    -- Khóa ngoại
    CONSTRAINT FK_Incidents_Rooms FOREIGN KEY (room_id) REFERENCES Rooms(room_id)
);
GO

-- 15. Tạo bảng MeterReadings (Chỉ số điện nước theo phòng)
CREATE TABLE MeterReadings (
    reading_id     INT IDENTITY(1,1) PRIMARY KEY,
    room_id        INT NOT NULL,
    billing_month  DATE NOT NULL,
    start_index    DECIMAL(12,2) NOT NULL,
    end_index      DECIMAL(12,2) NOT NULL,
    consumption    DECIMAL(12,2) NOT NULL,
    read_date      DATE NOT NULL,

    -- Khóa ngoại
    CONSTRAINT FK_MeterReadings_Rooms FOREIGN KEY (room_id) REFERENCES Rooms(room_id)
);
GO

