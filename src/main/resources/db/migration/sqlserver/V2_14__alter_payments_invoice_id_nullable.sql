-- Cho phép invoice_id nhận giá trị NULL cho các khoản thanh toán tiền cọc theo hợp đồng (deposit payment)
ALTER TABLE dbo.payments ALTER COLUMN invoice_id INT NULL;
ALTER TABLE dbo.payments ALTER COLUMN method VARCHAR(50) NOT NULL;
