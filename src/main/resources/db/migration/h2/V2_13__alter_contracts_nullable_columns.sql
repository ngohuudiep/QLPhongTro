-- Cho phép end_date và representative_id nhận giá trị NULL cho hợp đồng nháp (DRAFT) từ yêu cầu thuê trực tuyến
ALTER TABLE contracts ALTER COLUMN end_date DATE NULL;
ALTER TABLE contracts ALTER COLUMN representative_id INT NULL;
