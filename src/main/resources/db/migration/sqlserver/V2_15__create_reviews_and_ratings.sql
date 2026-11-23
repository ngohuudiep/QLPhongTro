SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

-- 1. Thêm cột rating vào bảng rooms và room_listings
ALTER TABLE dbo.rooms ADD average_rating DECIMAL(3, 2) NOT NULL CONSTRAINT df_rooms_avg_rating DEFAULT 0.00;
ALTER TABLE dbo.rooms ADD total_reviews INT NOT NULL CONSTRAINT df_rooms_total_reviews DEFAULT 0;

ALTER TABLE dbo.room_listings ADD average_rating DECIMAL(3, 2) NOT NULL CONSTRAINT df_room_listings_avg_rating DEFAULT 0.00;
ALTER TABLE dbo.room_listings ADD total_reviews INT NOT NULL CONSTRAINT df_room_listings_total_reviews DEFAULT 0;

-- 2. Thêm cột rating và huy hiệu vào bảng users
ALTER TABLE dbo.users ADD average_rating DECIMAL(3, 2) NOT NULL CONSTRAINT df_users_avg_rating DEFAULT 0.00;
ALTER TABLE dbo.users ADD total_reviews INT NOT NULL CONSTRAINT df_users_total_reviews DEFAULT 0;
ALTER TABLE dbo.users ADD is_top_landlord BIT NOT NULL CONSTRAINT df_users_top_landlord DEFAULT 0;

-- 3. Tạo bảng reviews
CREATE TABLE dbo.reviews (
    review_id INT IDENTITY(1,1) NOT NULL,
    contract_id INT NOT NULL,
    room_id INT NOT NULL,
    tenant_user_id INT NOT NULL,
    landlord_user_id INT NOT NULL,
    rating INT NOT NULL,
    comment NVARCHAR(2000) NOT NULL,
    reply_comment NVARCHAR(2000) NULL,
    replied_at DATETIME2 NULL,
    status VARCHAR(20) NOT NULL CONSTRAINT df_reviews_status DEFAULT 'VISIBLE',
    created_at DATETIME2 NOT NULL CONSTRAINT df_reviews_created_at DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME2 NULL,

    CONSTRAINT pk_reviews PRIMARY KEY (review_id),
    CONSTRAINT uq_reviews_contract_id UNIQUE (contract_id),
    CONSTRAINT chk_reviews_rating CHECK (rating >= 1 AND rating <= 5),
    CONSTRAINT fk_reviews_contract FOREIGN KEY (contract_id) REFERENCES dbo.contracts (contract_id),
    CONSTRAINT fk_reviews_room FOREIGN KEY (room_id) REFERENCES dbo.rooms (room_id),
    CONSTRAINT fk_reviews_tenant FOREIGN KEY (tenant_user_id) REFERENCES dbo.users (user_id),
    CONSTRAINT fk_reviews_landlord FOREIGN KEY (landlord_user_id) REFERENCES dbo.users (user_id)
);

-- 4. Tạo chỉ mục tối ưu truy vấn danh sách & sắp xếp xếp hạng
CREATE INDEX idx_reviews_room_status ON dbo.reviews(room_id, status);
CREATE INDEX idx_reviews_landlord_status ON dbo.reviews(landlord_user_id, status);
CREATE INDEX idx_reviews_tenant ON dbo.reviews(tenant_user_id);
CREATE INDEX idx_rooms_rating ON dbo.rooms(average_rating DESC, total_reviews DESC);
CREATE INDEX idx_room_listings_rating ON dbo.room_listings(average_rating DESC, total_reviews DESC);
CREATE INDEX idx_users_landlord_rating ON dbo.users(is_top_landlord DESC, average_rating DESC, total_reviews DESC);
