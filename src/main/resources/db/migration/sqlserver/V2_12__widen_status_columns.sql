-- ====================================================================
-- V2_12: Widen status columns across operational tables to prevent truncation
-- (Fix for SQL Server Error 2628: 'String or binary data would be truncated')
-- ====================================================================

SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

-- 1. booking_requests.status (supports PENDING_LANDLORD_REVIEW, APPROVED_BY_LANDLORD, etc.)
ALTER TABLE booking_requests ALTER COLUMN status VARCHAR(50) NOT NULL;

-- 2. contracts.status (supports TENANT_REJECTED, CANCELLED_BY_LANDLORD, etc.)
ALTER TABLE contracts ALTER COLUMN status VARCHAR(50) NOT NULL;

-- 3. incidents.status
ALTER TABLE incidents ALTER COLUMN status VARCHAR(50) NOT NULL;

-- 4. rooms.status & room_listings.status
ALTER TABLE rooms ALTER COLUMN status VARCHAR(50) NULL;
ALTER TABLE room_listings ALTER COLUMN status VARCHAR(50) NOT NULL;
