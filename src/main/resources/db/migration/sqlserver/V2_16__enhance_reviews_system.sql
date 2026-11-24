-- ====================================================================
-- V2_16: Thêm các cột phục vụ lý do ẩn và báo cáo vi phạm đánh giá
-- ====================================================================

SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.reviews') AND name = 'hide_reason')
BEGIN
    ALTER TABLE dbo.reviews ADD hide_reason NVARCHAR(500) NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.reviews') AND name = 'is_reported')
BEGIN
    ALTER TABLE dbo.reviews ADD is_reported BIT NOT NULL CONSTRAINT df_reviews_is_reported DEFAULT 0;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.reviews') AND name = 'report_reason')
BEGIN
    ALTER TABLE dbo.reviews ADD report_reason NVARCHAR(500) NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.reviews') AND name = 'reported_at')
BEGIN
    ALTER TABLE dbo.reviews ADD reported_at DATETIME2 NULL;
END;
