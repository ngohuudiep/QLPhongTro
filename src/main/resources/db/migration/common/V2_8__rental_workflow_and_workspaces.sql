-- ============================================================================
-- V2_8__rental_workflow_and_workspaces.sql
-- Thêm các bảng và cột phục vụ Workspace (Admin, Landlord, Tenant),
-- Rental Workflow và Contract Termination & Settlement.
-- Cú pháp ANSI SQL tương thích cả SQL Server và H2
-- ============================================================================

-- 1. Bổ sung các cột cho bảng users
ALTER TABLE users ADD account_status VARCHAR(30) NOT NULL DEFAULT 'APPROVED';
ALTER TABLE users ADD contact_address NVARCHAR(255) NULL;
ALTER TABLE users ADD approved_at DATETIME2 NULL;
ALTER TABLE users ADD approved_by_user_id INT NULL;
ALTER TABLE users ADD rejected_at DATETIME2 NULL;
ALTER TABLE users ADD rejection_reason NVARCHAR(1000) NULL;
ALTER TABLE users ADD registered_at DATETIME2 NULL;
ALTER TABLE users ADD enabled BIT NOT NULL DEFAULT 1;

ALTER TABLE users ADD CONSTRAINT fk_users_approved_by
    FOREIGN KEY (approved_by_user_id) REFERENCES users (user_id);

-- 2. Bổ sung các cột cho bảng buildings
ALTER TABLE buildings ADD description NVARCHAR(MAX) NULL;
ALTER TABLE buildings ADD contact_phone VARCHAR(20) NULL;
ALTER TABLE buildings ADD owner_user_id INT NULL;

ALTER TABLE buildings ADD CONSTRAINT fk_buildings_owner
    FOREIGN KEY (owner_user_id) REFERENCES users (user_id);

-- 3. Bổ sung các cột cho bảng booking_requests
ALTER TABLE booking_requests ADD full_name NVARCHAR(150) NULL;
ALTER TABLE booking_requests ADD phone VARCHAR(20) NULL;
ALTER TABLE booking_requests ADD id_number VARCHAR(20) NULL;
ALTER TABLE booking_requests ADD expected_occupants SMALLINT NULL;
ALTER TABLE booking_requests ADD expected_move_in_date DATE NULL;
ALTER TABLE booking_requests ADD expires_at DATETIME2 NULL;

-- 4. Bổ sung các cột cho bảng contracts
ALTER TABLE contracts ADD actual_end_date DATE NULL;
ALTER TABLE contracts ADD rental_request_id INT NULL;
ALTER TABLE contracts ADD tenant_user_id INT NULL;
ALTER TABLE contracts ADD tenant_accepted_at DATETIME2 NULL;
ALTER TABLE contracts ADD landlord_approved_at DATETIME2 NULL;
ALTER TABLE contracts ADD tenant_rejection_reason NVARCHAR(MAX) NULL;
ALTER TABLE contracts ADD landlord_rejection_reason NVARCHAR(MAX) NULL;
ALTER TABLE contracts ADD contract_type NVARCHAR(50) NULL;
ALTER TABLE contracts ADD contract_date DATE NULL;
ALTER TABLE contracts ADD payment_day SMALLINT NULL;
ALTER TABLE contracts ADD monthly_rent DECIMAL(14,2) NULL;
ALTER TABLE contracts ADD monthly_electric_price DECIMAL(14,2) NULL;
ALTER TABLE contracts ADD monthly_water_price DECIMAL(14,2) NULL;
ALTER TABLE contracts ADD internet_fee DECIMAL(14,2) NULL;
ALTER TABLE contracts ADD service_fee DECIMAL(14,2) NULL;
ALTER TABLE contracts ADD sanitation_fee DECIMAL(14,2) NULL;
ALTER TABLE contracts ADD other_service_fee DECIMAL(14,2) NULL;
ALTER TABLE contracts ADD payment_method NVARCHAR(100) NULL;
ALTER TABLE contracts ADD payment_terms NVARCHAR(MAX) NULL;
ALTER TABLE contracts ADD deposit_terms NVARCHAR(MAX) NULL;
ALTER TABLE contracts ADD utility_terms NVARCHAR(MAX) NULL;
ALTER TABLE contracts ADD asset_terms NVARCHAR(MAX) NULL;
ALTER TABLE contracts ADD repair_terms NVARCHAR(MAX) NULL;
ALTER TABLE contracts ADD termination_terms NVARCHAR(MAX) NULL;
ALTER TABLE contracts ADD additional_terms NVARCHAR(MAX) NULL;
ALTER TABLE contracts ADD occupant_terms NVARCHAR(MAX) NULL;
ALTER TABLE contracts ADD renewal_terms NVARCHAR(MAX) NULL;
ALTER TABLE contracts ADD landlord_responsibilities NVARCHAR(MAX) NULL;
ALTER TABLE contracts ADD tenant_responsibilities NVARCHAR(MAX) NULL;

ALTER TABLE contracts ADD CONSTRAINT fk_contracts_rental_request
    FOREIGN KEY (rental_request_id) REFERENCES booking_requests (request_id);

ALTER TABLE contracts ADD CONSTRAINT fk_contracts_tenant_user
    FOREIGN KEY (tenant_user_id) REFERENCES users (user_id);

-- 5. Bổ sung các cột cho bảng payments
ALTER TABLE payments ADD contract_id INT NULL;
ALTER TABLE payments ADD transaction_code NVARCHAR(80) NULL;
ALTER TABLE payments ADD status NVARCHAR(20) NULL;
ALTER TABLE payments ADD provider NVARCHAR(30) NULL;
ALTER TABLE payments ADD created_at DATETIME2 NULL;
ALTER TABLE payments ADD processed_at DATETIME2 NULL;

ALTER TABLE payments ADD CONSTRAINT fk_payments_contract
    FOREIGN KEY (contract_id) REFERENCES contracts (contract_id);

-- 6. Bổ sung các cột cho bảng invoices
ALTER TABLE invoices ADD created_at DATETIME2 NULL;
ALTER TABLE invoices ADD notes NVARCHAR(MAX) NULL;

-- 7. Bổ sung các cột cho bảng incidents
ALTER TABLE incidents ADD description NVARCHAR(MAX) NULL;
ALTER TABLE incidents ADD reported_by_user_id INT NULL;

ALTER TABLE incidents ADD CONSTRAINT fk_incidents_reported_by_user
    FOREIGN KEY (reported_by_user_id) REFERENCES users (user_id);

-- 8. Bổ sung các cột cho bảng room_listings
ALTER TABLE room_listings ADD managed_room_id INT NULL;

ALTER TABLE room_listings ADD CONSTRAINT fk_room_listings_managed_room
    FOREIGN KEY (managed_room_id) REFERENCES rooms (room_id);

-- Đồng bộ dữ liệu phòng giữa room_id và managed_room_id
UPDATE room_listings SET managed_room_id = room_id WHERE managed_room_id IS NULL AND room_id IS NOT NULL;
UPDATE room_listings SET room_id = managed_room_id WHERE room_id IS NULL AND managed_room_id IS NOT NULL;

-- 9. Tạo bảng invoice_line_items
CREATE TABLE invoice_line_items (
    line_item_id INT IDENTITY(1,1) NOT NULL,
    invoice_id INT NOT NULL,
    line_name NVARCHAR(120) NOT NULL,
    quantity DECIMAL(14,3) NOT NULL,
    unit NVARCHAR(30) NOT NULL,
    unit_price DECIMAL(14,2) NOT NULL,
    amount DECIMAL(14,2) NOT NULL,
    previous_reading DECIMAL(14,3) NULL,
    current_reading DECIMAL(14,3) NULL,
    CONSTRAINT pk_invoice_line_items PRIMARY KEY (line_item_id),
    CONSTRAINT fk_invoice_line_items_invoices
        FOREIGN KEY (invoice_id) REFERENCES invoices (invoice_id)
);

-- 10. Tạo bảng contract_termination_requests
CREATE TABLE contract_termination_requests (
    termination_request_id INT IDENTITY(1,1) NOT NULL,
    contract_id INT NOT NULL,
    requester_user_id INT NOT NULL,
    responder_user_id INT NOT NULL,
    requester_role NVARCHAR(20) NOT NULL,
    original_contract_status NVARCHAR(30) NOT NULL,
    reason_code NVARCHAR(60) NOT NULL,
    reason_details NVARCHAR(1500) NOT NULL,
    notes NVARCHAR(2000) NULL,
    response_reason NVARCHAR(1500) NULL,
    expected_termination_date DATE NOT NULL,
    status NVARCHAR(30) NOT NULL,
    created_at DATETIME2 NOT NULL,
    responded_at DATETIME2 NULL,
    completed_at DATETIME2 NULL,
    CONSTRAINT pk_contract_termination_requests PRIMARY KEY (termination_request_id),
    CONSTRAINT fk_termination_requests_contract
        FOREIGN KEY (contract_id) REFERENCES contracts (contract_id),
    CONSTRAINT fk_termination_requests_requester
        FOREIGN KEY (requester_user_id) REFERENCES users (user_id),
    CONSTRAINT fk_termination_requests_responder
        FOREIGN KEY (responder_user_id) REFERENCES users (user_id)
);

-- 11. Tạo bảng contract_settlements
CREATE TABLE contract_settlements (
    settlement_id INT IDENTITY(1,1) NOT NULL,
    termination_request_id INT NOT NULL,
    unpaid_invoice_amount DECIMAL(14,2) NULL,
    paid_invoice_amount DECIMAL(14,2) NULL,
    rent_through_move_out_amount DECIMAL(14,2) NULL,
    electricity_old_reading DECIMAL(14,3) NULL,
    electricity_new_reading DECIMAL(14,3) NULL,
    electricity_unit_price DECIMAL(14,2) NULL,
    electricity_amount DECIMAL(14,2) NULL,
    water_old_reading DECIMAL(14,3) NULL,
    water_new_reading DECIMAL(14,3) NULL,
    water_unit_price DECIMAL(14,2) NULL,
    water_amount DECIMAL(14,2) NULL,
    service_fee_amount DECIMAL(14,2) NULL,
    compensation_amount DECIMAL(14,2) NULL,
    compensation_reason NVARCHAR(1000) NULL,
    deposit_amount DECIMAL(14,2) NULL,
    deposit_deduction_amount DECIMAL(14,2) NULL,
    deposit_deduction_reason NVARCHAR(1000) NULL,
    deposit_refund_amount DECIMAL(14,2) NULL,
    total_due_amount DECIMAL(14,2) NULL,
    room_handover_condition NVARCHAR(1500) NULL,
    damage_notes NVARCHAR(1500) NULL,
    notes NVARCHAR(2000) NULL,
    status NVARCHAR(30) NOT NULL,
    landlord_confirmed_at DATETIME2 NULL,
    tenant_confirmed_at DATETIME2 NULL,
    disputed_at DATETIME2 NULL,
    dispute_reason NVARCHAR(1500) NULL,
    tenant_payment_reference NVARCHAR(100) NULL,
    tenant_payment_confirmed_at DATETIME2 NULL,
    deposit_refund_reference NVARCHAR(100) NULL,
    deposit_refunded_at DATETIME2 NULL,
    updated_at DATETIME2 NOT NULL,
    CONSTRAINT pk_contract_settlements PRIMARY KEY (settlement_id),
    CONSTRAINT uq_contract_settlements_termination_request UNIQUE (termination_request_id),
    CONSTRAINT fk_contract_settlements_termination_request
        FOREIGN KEY (termination_request_id) REFERENCES contract_termination_requests (termination_request_id)
);

-- 12. Tạo bảng contract_termination_histories
CREATE TABLE contract_termination_histories (
    history_id INT IDENTITY(1,1) NOT NULL,
    termination_request_id INT NOT NULL,
    contract_id INT NOT NULL,
    actor_user_id INT NOT NULL,
    action VARCHAR(50) NOT NULL,
    previous_status NVARCHAR(30) NULL,
    new_status NVARCHAR(30) NULL,
    details NVARCHAR(2000) NULL,
    happened_at DATETIME2 NOT NULL,
    CONSTRAINT pk_contract_termination_histories PRIMARY KEY (history_id),
    CONSTRAINT fk_termination_histories_request
        FOREIGN KEY (termination_request_id) REFERENCES contract_termination_requests (termination_request_id),
    CONSTRAINT fk_termination_histories_contract
        FOREIGN KEY (contract_id) REFERENCES contracts (contract_id),
    CONSTRAINT fk_termination_histories_actor
        FOREIGN KEY (actor_user_id) REFERENCES users (user_id)
);

-- 13. Tạo bảng deposit_confirmation_requests
CREATE TABLE deposit_confirmation_requests (
    request_id INT IDENTITY(1,1) NOT NULL,
    contract_id INT NOT NULL,
    tenant_user_id INT NOT NULL,
    landlord_user_id INT NOT NULL,
    amount DECIMAL(14,2) NOT NULL,
    request_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at DATETIME2 NOT NULL,
    responded_at DATETIME2 NULL,
    responded_by_user_id INT NULL,
    rejection_reason NVARCHAR(MAX) NULL,
    response_reference VARCHAR(50) NULL,
    payment_id INT NULL,
    CONSTRAINT pk_deposit_confirmation_requests PRIMARY KEY (request_id),
    CONSTRAINT fk_deposit_confirmation_contract
        FOREIGN KEY (contract_id) REFERENCES contracts (contract_id),
    CONSTRAINT fk_deposit_confirmation_tenant
        FOREIGN KEY (tenant_user_id) REFERENCES users (user_id),
    CONSTRAINT fk_deposit_confirmation_landlord
        FOREIGN KEY (landlord_user_id) REFERENCES users (user_id),
    CONSTRAINT fk_deposit_confirmation_responder
        FOREIGN KEY (responded_by_user_id) REFERENCES users (user_id),
    CONSTRAINT fk_deposit_confirmation_payment
        FOREIGN KEY (payment_id) REFERENCES payments (payment_id)
);

-- 14. Tạo bảng user_notifications
CREATE TABLE user_notifications (
    notification_id INT IDENTITY(1,1) NOT NULL,
    recipient_user_id INT NOT NULL,
    invoice_id INT NULL,
    contract_id INT NULL,
    rental_request_id INT NULL,
    termination_request_id INT NULL,
    notification_type NVARCHAR(30) NOT NULL,
    title NVARCHAR(150) NOT NULL,
    message NVARCHAR(MAX) NOT NULL,
    created_at DATETIME2 NOT NULL,
    read_at DATETIME2 NULL,
    CONSTRAINT pk_user_notifications PRIMARY KEY (notification_id),
    CONSTRAINT fk_user_notifications_recipient
        FOREIGN KEY (recipient_user_id) REFERENCES users (user_id),
    CONSTRAINT fk_user_notifications_invoices
        FOREIGN KEY (invoice_id) REFERENCES invoices (invoice_id),
    CONSTRAINT fk_user_notifications_contracts
        FOREIGN KEY (contract_id) REFERENCES contracts (contract_id),
    CONSTRAINT fk_user_notifications_rental_requests
        FOREIGN KEY (rental_request_id) REFERENCES booking_requests (request_id),
    CONSTRAINT fk_user_notifications_termination_requests
        FOREIGN KEY (termination_request_id) REFERENCES contract_termination_requests (termination_request_id)
);
