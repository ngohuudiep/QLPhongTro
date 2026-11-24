-- 1. Thêm cột rating vào bảng rooms và room_listings
ALTER TABLE rooms ADD average_rating DECIMAL(3, 2) DEFAULT 0.00 NOT NULL;
ALTER TABLE rooms ADD total_reviews INT DEFAULT 0 NOT NULL;

ALTER TABLE room_listings ADD average_rating DECIMAL(3, 2) DEFAULT 0.00 NOT NULL;
ALTER TABLE room_listings ADD total_reviews INT DEFAULT 0 NOT NULL;

-- 2. Thêm cột rating và huy hiệu vào bảng users
ALTER TABLE users ADD average_rating DECIMAL(3, 2) DEFAULT 0.00 NOT NULL;
ALTER TABLE users ADD total_reviews INT DEFAULT 0 NOT NULL;
ALTER TABLE users ADD is_top_landlord BIT DEFAULT 0 NOT NULL;

-- 3. Tạo bảng reviews
CREATE TABLE reviews (
    review_id INT IDENTITY(1,1) NOT NULL,
    contract_id INT NOT NULL,
    room_id INT NOT NULL,
    tenant_user_id INT NOT NULL,
    landlord_user_id INT NOT NULL,
    rating INT NOT NULL,
    comment NVARCHAR(2000) NOT NULL,
    reply_comment NVARCHAR(2000) NULL,
    replied_at TIMESTAMP NULL,
    status VARCHAR(20) DEFAULT 'VISIBLE' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL,

    CONSTRAINT pk_reviews PRIMARY KEY (review_id),
    CONSTRAINT uq_reviews_contract_id UNIQUE (contract_id),
    CONSTRAINT chk_reviews_rating CHECK (rating >= 1 AND rating <= 5),
    CONSTRAINT fk_reviews_contract FOREIGN KEY (contract_id) REFERENCES contracts (contract_id),
    CONSTRAINT fk_reviews_room FOREIGN KEY (room_id) REFERENCES rooms (room_id),
    CONSTRAINT fk_reviews_tenant FOREIGN KEY (tenant_user_id) REFERENCES users (user_id),
    CONSTRAINT fk_reviews_landlord FOREIGN KEY (landlord_user_id) REFERENCES users (user_id)
);

-- 4. Tạo chỉ mục tối ưu truy vấn danh sách & sắp xếp xếp hạng
CREATE INDEX idx_reviews_room_status ON reviews(room_id, status);
CREATE INDEX idx_reviews_landlord_status ON reviews(landlord_user_id, status);
CREATE INDEX idx_reviews_tenant ON reviews(tenant_user_id);
CREATE INDEX idx_rooms_rating ON rooms(average_rating DESC, total_reviews DESC);
CREATE INDEX idx_room_listings_rating ON room_listings(average_rating DESC, total_reviews DESC);
CREATE INDEX idx_users_landlord_rating ON users(is_top_landlord DESC, average_rating DESC, total_reviews DESC);
