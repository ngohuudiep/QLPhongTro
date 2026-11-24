-- ====================================================================
-- V2_16: Enhance reviews table for H2
-- ====================================================================

ALTER TABLE reviews ADD COLUMN IF NOT EXISTS hide_reason VARCHAR(500);
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS is_reported BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS report_reason VARCHAR(500);
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS reported_at TIMESTAMP;
