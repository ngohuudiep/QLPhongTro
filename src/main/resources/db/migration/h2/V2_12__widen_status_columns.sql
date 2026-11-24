-- ====================================================================
-- V2_12: Widen status columns across operational tables (H2 dialect)
-- ====================================================================

ALTER TABLE booking_requests ALTER COLUMN status VARCHAR(50) NOT NULL;
ALTER TABLE contracts ALTER COLUMN status VARCHAR(50) NOT NULL;
ALTER TABLE incidents ALTER COLUMN status VARCHAR(50) NOT NULL;
ALTER TABLE rooms ALTER COLUMN status VARCHAR(50) NULL;
ALTER TABLE room_listings ALTER COLUMN status VARCHAR(50) NOT NULL;
