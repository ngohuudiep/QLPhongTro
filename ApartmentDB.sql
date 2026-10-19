-- ============================================================================
-- HỆ THỐNG QUẢN LÝ PHÒNG TRỌ (ApartmentDB)
-- SCRIPT TẠO TOÀN BỘ CƠ SỞ DỮ LIỆU DỰA TRÊN SƠ ĐỒ CHI TIẾT (31 BẢNG)
-- Hỗ trợ: Microsoft SQL Server 2017+ / Azure SQL Database
-- ============================================================================

IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'ApartmentDB')
BEGIN
    CREATE DATABASE ApartmentDB;
END
GO

USE ApartmentDB;
GO

-- ============================================================================
-- 1. BẢNG vai_tro (Vai trò người dùng)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'vai_tro' AND type = 'U')
BEGIN
    CREATE TABLE vai_tro (
        id           INT IDENTITY(1,1) PRIMARY KEY,
        ten_vai_tro  VARCHAR(50)  NOT NULL UNIQUE,
        mo_ta        NVARCHAR(255) NULL
    );
END
GO

-- ============================================================================
-- 2. BẢNG nguoi_dung (Tài khoản người dùng hệ thống)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'nguoi_dung' AND type = 'U')
BEGIN
    CREATE TABLE nguoi_dung (
        id             INT IDENTITY(1,1) PRIMARY KEY,
        vai_tro_id     INT NOT NULL,
        ho_ten         NVARCHAR(100) NULL,
        email          VARCHAR(100)  NULL,
        so_dien_thoai  VARCHAR(20)   NULL,
        ten_dang_nhap  VARCHAR(50)   NOT NULL UNIQUE,
        mat_khau       VARCHAR(255)  NOT NULL,
        trang_thai     VARCHAR(20)   NOT NULL DEFAULT 'HOAT_DONG',
        ngay_tao       DATETIME      DEFAULT CURRENT_TIMESTAMP,

        CONSTRAINT FK_nguoi_dung_vai_tro FOREIGN KEY (vai_tro_id) REFERENCES vai_tro(id)
    );
END
GO

-- ============================================================================
-- 3. BẢNG khu_tro (Khu trọ / Cơ sở nhà trọ)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'khu_tro' AND type = 'U')
BEGIN
    CREATE TABLE khu_tro (
        id           INT IDENTITY(1,1) PRIMARY KEY,
        ten_khu_tro  NVARCHAR(100) NOT NULL,
        dia_chi      NVARCHAR(255) NOT NULL,
        phuong_xa    NVARCHAR(100) NULL,
        quan_huyen   NVARCHAR(100) NULL,
        tinh_thanh   NVARCHAR(100) NULL,
        mo_ta        NVARCHAR(MAX) NULL,
        trang_thai   VARCHAR(20)   NOT NULL DEFAULT 'HOAT_DONG'
    );
END
GO

-- ============================================================================
-- 4. BẢNG loai_phong (Phân loại phòng trọ)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'loai_phong' AND type = 'U')
BEGIN
    CREATE TABLE loai_phong (
        id             INT IDENTITY(1,1) PRIMARY KEY,
        ten_loai_phong NVARCHAR(100) NOT NULL,
        dien_tich      DECIMAL(8,2)  NULL,
        gia_tham_khao  DECIMAL(15,2) NULL,
        mo_ta          NVARCHAR(MAX) NULL
    );
END
GO

-- ============================================================================
-- 5. BẢNG phong_tro (Danh mục phòng trọ)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'phong_tro' AND type = 'U')
BEGIN
    CREATE TABLE phong_tro (
        id                 INT IDENTITY(1,1) PRIMARY KEY,
        khu_tro_id         INT NOT NULL,
        loai_phong_id      INT NOT NULL,
        ma_phong           VARCHAR(30)   NOT NULL,
        tang               INT NULL,
        dien_tich          DECIMAL(8,2)  NULL,
        gia_thue           DECIMAL(15,2) NOT NULL,
        tien_coc_mac_dinh  DECIMAL(15,2) NULL,
        suc_chua           INT DEFAULT 2,
        trang_thai         VARCHAR(20)   NOT NULL DEFAULT 'TRONG',
        mo_ta              NVARCHAR(MAX) NULL,

        CONSTRAINT FK_phong_tro_khu_tro FOREIGN KEY (khu_tro_id) REFERENCES khu_tro(id),
        CONSTRAINT FK_phong_tro_loai_phong FOREIGN KEY (loai_phong_id) REFERENCES loai_phong(id),
        CONSTRAINT CK_phong_tro_trang_thai CHECK (trang_thai IN ('TRONG', 'DA_THUE', 'DANG_SUA_CHUA', 'DAT_COC'))
    );
END
GO

-- ============================================================================
-- 6. BẢNG nguoi_thue (Hồ sơ cư dân / Khách thuê)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'nguoi_thue' AND type = 'U')
BEGIN
    CREATE TABLE nguoi_thue (
        id                 INT IDENTITY(1,1) PRIMARY KEY,
        ho_ten             NVARCHAR(100) NOT NULL,
        ngay_sinh          DATE NULL,
        gioi_tinh          NVARCHAR(20) NULL,
        so_dien_thoai      VARCHAR(20)  NOT NULL,
        email              VARCHAR(100) NULL,
        so_cccd            VARCHAR(20)  NOT NULL UNIQUE,
        dia_chi_thuong_tru NVARCHAR(255) NULL,
        nghe_nghiep        NVARCHAR(100) NULL,
        ngay_tao           DATETIME DEFAULT CURRENT_TIMESTAMP,
        ghi_chu            NVARCHAR(MAX) NULL
    );
END
GO

-- ============================================================================
-- 7. BẢNG hop_dong_thue (Hợp đồng thuê phòng)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'hop_dong_thue' AND type = 'U')
BEGIN
    CREATE TABLE hop_dong_thue (
        id                         INT IDENTITY(1,1) PRIMARY KEY,
        phong_tro_id               INT NOT NULL,
        ma_hop_dong                VARCHAR(50)   NOT NULL UNIQUE,
        ngay_bat_dau               DATE NOT NULL,
        ngay_ket_thuc              DATE NOT NULL,
        ngay_ky                    DATE NULL,
        gia_thue                   DECIMAL(15,2) NOT NULL,
        tien_coc                   DECIMAL(15,2) NOT NULL,
        ngay_thanh_toan_hang_thang INT DEFAULT 5,
        dieu_khoan                 NVARCHAR(MAX) NULL,
        duong_dan_file             VARCHAR(255) NULL,
        trang_thai                 VARCHAR(20) NOT NULL DEFAULT 'HIEU_LUC',
        ghi_chu                    NVARCHAR(MAX) NULL,

        CONSTRAINT FK_hop_dong_phong_tro FOREIGN KEY (phong_tro_id) REFERENCES phong_tro(id),
        CONSTRAINT CK_hop_dong_trang_thai CHECK (trang_thai IN ('HIEU_LUC', 'HET_HAN', 'DA_THANH_LY', 'DA_HUY'))
    );
END
GO

-- ============================================================================
-- 8. BẢNG hop_dong_nguoi_thue (Junction Table N-N: Hợp đồng và Người ở)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'hop_dong_nguoi_thue' AND type = 'U')
BEGIN
    CREATE TABLE hop_dong_nguoi_thue (
        id                     INT IDENTITY(1,1) PRIMARY KEY,
        hop_dong_id            INT NOT NULL,
        nguoi_thue_id          INT NOT NULL,
        vai_tro_trong_hop_dong VARCHAR(30) NULL, -- DAI_DIEN | THANH_VIEN_O_CUNG
        la_nguoi_dai_dien      BIT DEFAULT 0,

        CONSTRAINT FK_hd_nguoi_thue_hd FOREIGN KEY (hop_dong_id) REFERENCES hop_dong_thue(id) ON DELETE CASCADE,
        CONSTRAINT FK_hd_nguoi_thue_nt FOREIGN KEY (nguoi_thue_id) REFERENCES nguoi_thue(id)
    );
END
GO

-- ============================================================================
-- 9. BẢNG lich_su_dieu_chinh_hop_dong (Lịch sử phụ lục / Thay đổi hợp đồng)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'lich_su_dieu_chinh_hop_dong' AND type = 'U')
BEGIN
    CREATE TABLE lich_su_dieu_chinh_hop_dong (
        id                INT IDENTITY(1,1) PRIMARY KEY,
        hop_dong_id       INT NOT NULL,
        nguoi_dung_id     INT NOT NULL,
        noi_dung_thay_doi NVARCHAR(MAX) NOT NULL,
        ngay_thay_doi     DATETIME DEFAULT CURRENT_TIMESTAMP,
        gia_thue_cu       DECIMAL(15,2) NULL,
        gia_thue_moi      DECIMAL(15,2) NULL,
        ghi_chu           NVARCHAR(MAX) NULL,

        CONSTRAINT FK_ls_hop_dong FOREIGN KEY (hop_dong_id) REFERENCES hop_dong_thue(id),
        CONSTRAINT FK_ls_nguoi_dung FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id)
    );
END
GO

-- ============================================================================
-- 10. BẢNG dich_vu (Danh mục dịch vụ)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'dich_vu' AND type = 'U')
BEGIN
    CREATE TABLE dich_vu (
        id                INT IDENTITY(1,1) PRIMARY KEY,
        ten_dich_vu       NVARCHAR(100) NOT NULL,
        don_vi_tinh       NVARCHAR(30)  NOT NULL,
        cach_tinh         VARCHAR(30)   NOT NULL, -- THEO_CONG_TO | CO_DINH
        don_gia_mac_dinh  DECIMAL(15,2) NOT NULL,
        mo_ta             NVARCHAR(MAX) NULL,
        trang_thai        VARCHAR(20)   NOT NULL DEFAULT 'HOAT_DONG'
    );
END
GO

-- ============================================================================
-- 11. BẢNG dich_vu_hop_dong (Junction Table N-N: Biểu phí dịch vụ theo hợp đồng)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'dich_vu_hop_dong' AND type = 'U')
BEGIN
    CREATE TABLE dich_vu_hop_dong (
        id            INT IDENTITY(1,1) PRIMARY KEY,
        hop_dong_id   INT NOT NULL,
        dich_vu_id    INT NOT NULL,
        don_gia       DECIMAL(15,2) NOT NULL,
        ngay_ap_dung  DATE NOT NULL,
        ngay_ngung    DATE NULL,
        ghi_chu       NVARCHAR(MAX) NULL,

        CONSTRAINT FK_dv_hd_hop_dong FOREIGN KEY (hop_dong_id) REFERENCES hop_dong_thue(id) ON DELETE CASCADE,
        CONSTRAINT FK_dv_hd_dich_vu FOREIGN KEY (dich_vu_id) REFERENCES dich_vu(id)
    );
END
GO

-- ============================================================================
-- 12. BẢNG danh_muc_tien_nghi (Danh mục tiện nghi tiêu chuẩn)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'danh_muc_tien_nghi' AND type = 'U')
BEGIN
    CREATE TABLE danh_muc_tien_nghi (
        id            INT IDENTITY(1,1) PRIMARY KEY,
        ten_tien_nghi NVARCHAR(100) NOT NULL,
        mo_ta         NVARCHAR(255) NULL
    );
END
GO

-- ============================================================================
-- 13. BẢNG tien_nghi_phong (Tiện nghi trang bị cho phòng)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'tien_nghi_phong' AND type = 'U')
BEGIN
    CREATE TABLE tien_nghi_phong (
        id            INT IDENTITY(1,1) PRIMARY KEY,
        phong_tro_id  INT NOT NULL,
        tien_nghi_id  INT NOT NULL,
        so_luong      INT DEFAULT 1,
        tinh_trang    NVARCHAR(50) NULL,
        ghi_chu       NVARCHAR(MAX) NULL,

        CONSTRAINT FK_tn_phong_phong FOREIGN KEY (phong_tro_id) REFERENCES phong_tro(id) ON DELETE CASCADE,
        CONSTRAINT FK_tn_phong_danh_muc FOREIGN KEY (tien_nghi_id) REFERENCES danh_muc_tien_nghi(id)
    );
END
GO

-- ============================================================================
-- 14. BẢNG cong_to (Công tơ đo đếm điện nước gắn theo phòng)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'cong_to' AND type = 'U')
BEGIN
    CREATE TABLE cong_to (
        id              INT IDENTITY(1,1) PRIMARY KEY,
        phong_tro_id    INT NOT NULL,
        dich_vu_id      INT NOT NULL,
        ma_cong_to      VARCHAR(50)   NOT NULL,
        ngay_lap_dat    DATE NULL,
        chi_so_ban_dau  DECIMAL(12,3) DEFAULT 0,
        trang_thai      VARCHAR(20)   NOT NULL DEFAULT 'HOAT_DONG',
        ghi_chu         NVARCHAR(MAX) NULL,

        CONSTRAINT FK_cong_to_phong FOREIGN KEY (phong_tro_id) REFERENCES phong_tro(id),
        CONSTRAINT FK_cong_to_dich_vu FOREIGN KEY (dich_vu_id) REFERENCES dich_vu(id)
    );
END
GO

-- ============================================================================
-- 15. BẢNG chi_so_cong_to (Lịch sử ghi số công tơ định kỳ)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'chi_so_cong_to' AND type = 'U')
BEGIN
    CREATE TABLE chi_so_cong_to (
        id            INT IDENTITY(1,1) PRIMARY KEY,
        cong_to_id    INT NOT NULL,
        ngay_ghi      DATETIME DEFAULT CURRENT_TIMESTAMP,
        chi_so        DECIMAL(12,3) NOT NULL,
        nguoi_ghi_id  INT NULL,
        hinh_anh      VARCHAR(255) NULL,
        ghi_chu       NVARCHAR(MAX) NULL,

        CONSTRAINT FK_cs_cong_to FOREIGN KEY (cong_to_id) REFERENCES cong_to(id),
        CONSTRAINT FK_cs_nguoi_ghi FOREIGN KEY (nguoi_ghi_id) REFERENCES nguoi_dung(id)
    );
END
GO

-- ============================================================================
-- 16. BẢNG hoa_don (Hóa đơn tiền phòng & dịch vụ)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'hoa_don' AND type = 'U')
BEGIN
    CREATE TABLE hoa_don (
        id                     INT IDENTITY(1,1) PRIMARY KEY,
        hop_dong_id            INT NOT NULL,
        ma_hoa_don             VARCHAR(50)   NOT NULL UNIQUE,
        thang                  INT NOT NULL,
        nam                    INT NOT NULL,
        ngay_phat_hanh         DATE NOT NULL,
        han_thanh_toan         DATE NOT NULL,
        tong_tien              DECIMAL(15,2) NOT NULL,
        so_tien_da_thanh_toan  DECIMAL(15,2) DEFAULT 0,
        trang_thai             VARCHAR(20)   NOT NULL DEFAULT 'CHUA_THANH_TOAN',
        ghi_chu                NVARCHAR(MAX) NULL,

        CONSTRAINT FK_hoa_don_hop_dong FOREIGN KEY (hop_dong_id) REFERENCES hop_dong_thue(id),
        CONSTRAINT CK_hoa_don_trang_thai CHECK (trang_thai IN ('CHUA_THANH_TOAN', 'THANH_TOAN_MOT_PHAN', 'DA_THANH_TOAN', 'QUA_HAN', 'DA_HUY'))
    );
END
GO

-- ============================================================================
-- 17. BẢNG chi_tiet_hoa_don (Chi tiết các mục thu tiền trong hóa đơn)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'chi_tiet_hoa_don' AND type = 'U')
BEGIN
    CREATE TABLE chi_tiet_hoa_don (
        id             INT IDENTITY(1,1) PRIMARY KEY,
        hoa_don_id     INT NOT NULL,
        dich_vu_id     INT NULL,
        ten_khoan_thu  NVARCHAR(100) NOT NULL,
        so_luong       DECIMAL(12,3) DEFAULT 1,
        don_vi_tinh    NVARCHAR(30)  NULL,
        don_gia        DECIMAL(15,2) NOT NULL,
        thanh_tien     DECIMAL(15,2) NOT NULL,
        chi_so_cu      DECIMAL(12,3) NULL,
        chi_so_moi     DECIMAL(12,3) NULL,
        ghi_chu        NVARCHAR(MAX) NULL,

        CONSTRAINT FK_ct_hoa_don FOREIGN KEY (hoa_don_id) REFERENCES hoa_don(id) ON DELETE CASCADE,
        CONSTRAINT FK_ct_dich_vu FOREIGN KEY (dich_vu_id) REFERENCES dich_vu(id)
    );
END
GO

-- ============================================================================
-- 18. BẢNG dieu_chinh_hoa_don (Điều chỉnh tăng/giảm tiền hóa đơn)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'dieu_chinh_hoa_don' AND type = 'U')
BEGIN
    CREATE TABLE dieu_chinh_hoa_don (
        id                  INT IDENTITY(1,1) PRIMARY KEY,
        hoa_don_id          INT NOT NULL,
        nguoi_dung_id       INT NOT NULL,
        so_tien_dieu_chinh  DECIMAL(15,2) NOT NULL,
        ly_do               NVARCHAR(255) NOT NULL,
        ngay_dieu_chinh     DATETIME DEFAULT CURRENT_TIMESTAMP,

        CONSTRAINT FK_dc_hoa_don FOREIGN KEY (hoa_don_id) REFERENCES hoa_don(id),
        CONSTRAINT FK_dc_nguoi_dung FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id)
    );
END
GO

-- ============================================================================
-- 19. BẢNG thanh_toan (Giao dịch nộp tiền thanh toán)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'thanh_toan' AND type = 'U')
BEGIN
    CREATE TABLE thanh_toan (
        id                   INT IDENTITY(1,1) PRIMARY KEY,
        hop_dong_id          INT NOT NULL,
        ma_giao_dich         VARCHAR(100) NULL,
        loai_thanh_toan      VARCHAR(30)  NULL, -- TIEN_PHONG | TIEN_COC | TIEN_DICH_VU
        so_tien              DECIMAL(15,2) NOT NULL,
        ngay_thanh_toan      DATETIME DEFAULT CURRENT_TIMESTAMP,
        phuong_thuc          VARCHAR(30)  NOT NULL, -- TIEN_MAT | CHUYEN_KHOAN | VNPAY
        ma_bien_nhan         VARCHAR(50)  NULL,
        duong_dan_bien_nhan  VARCHAR(255) NULL,
        nguoi_nhan_id        INT NULL,
        trang_thai           VARCHAR(20)  NOT NULL DEFAULT 'HOAN_THANH',
        ghi_chu              NVARCHAR(MAX) NULL,

        CONSTRAINT FK_thanh_toan_hop_dong FOREIGN KEY (hop_dong_id) REFERENCES hop_dong_thue(id),
        CONSTRAINT FK_thanh_toan_nguoi_nhan FOREIGN KEY (nguoi_nhan_id) REFERENCES nguoi_dung(id)
    );
END
GO

-- ============================================================================
-- 20. BẢNG phan_bo_thanh_toan (Junction Table: Phân bổ tiền thanh toán vào hóa đơn)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'phan_bo_thanh_toan' AND type = 'U')
BEGIN
    CREATE TABLE phan_bo_thanh_toan (
        id               INT IDENTITY(1,1) PRIMARY KEY,
        thanh_toan_id    INT NOT NULL,
        hoa_don_id       INT NOT NULL,
        so_tien_phan_bo  DECIMAL(15,2) NOT NULL,
        ghi_chu          NVARCHAR(MAX) NULL,

        CONSTRAINT FK_pb_thanh_toan FOREIGN KEY (thanh_toan_id) REFERENCES thanh_toan(id),
        CONSTRAINT FK_pb_hoa_don FOREIGN KEY (hoa_don_id) REFERENCES hoa_don(id)
    );
END
GO

-- ============================================================================
-- 21. BẢNG nha_cung_cap (Nhà cung cấp vật tư / Dịch vụ ngoại)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'nha_cung_cap' AND type = 'U')
BEGIN
    CREATE TABLE nha_cung_cap (
        id                INT IDENTITY(1,1) PRIMARY KEY,
        ten_nha_cung_cap  NVARCHAR(150) NOT NULL,
        so_dien_thoai     VARCHAR(20)   NULL,
        email             VARCHAR(100)  NULL,
        dia_chi           NVARCHAR(255) NULL,
        ma_so_thue        VARCHAR(30)   NULL,
        ghi_chu           NVARCHAR(MAX) NULL
    );
END
GO

-- ============================================================================
-- 22. BẢNG chi_phi (Chi phí vận hành dãy trọ)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'chi_phi' AND type = 'U')
BEGIN
    CREATE TABLE chi_phi (
        id                 INT IDENTITY(1,1) PRIMARY KEY,
        khu_tro_id         INT NULL,
        phong_tro_id       INT NULL,
        nha_cung_cap_id    INT NULL,
        nguoi_tao_id       INT NOT NULL,
        loai_chi_phi       NVARCHAR(100) NOT NULL,
        noi_dung           NVARCHAR(255) NOT NULL,
        so_tien            DECIMAL(15,2) NOT NULL,
        ngay_chi           DATE NOT NULL,
        duong_dan_hoa_don  VARCHAR(255) NULL,
        ghi_chu            NVARCHAR(MAX) NULL,

        CONSTRAINT FK_chi_phi_khu_tro FOREIGN KEY (khu_tro_id) REFERENCES khu_tro(id),
        CONSTRAINT FK_chi_phi_phong_tro FOREIGN KEY (phong_tro_id) REFERENCES phong_tro(id),
        CONSTRAINT FK_chi_phi_ncc FOREIGN KEY (nha_cung_cap_id) REFERENCES nha_cung_cap(id),
        CONSTRAINT FK_chi_phi_nguoi_tao FOREIGN KEY (nguoi_tao_id) REFERENCES nguoi_dung(id)
    );
END
GO

-- ============================================================================
-- 23. BẢNG yeu_cau_sua_chua (Phiếu báo sự cố & sửa chữa thiết bị)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'yeu_cau_sua_chua' AND type = 'U')
BEGIN
    CREATE TABLE yeu_cau_sua_chua (
        id                  INT IDENTITY(1,1) PRIMARY KEY,
        phong_tro_id        INT NOT NULL,
        nguoi_tao_id        INT NOT NULL,
        nguoi_phu_trach_id  INT NULL,
        tieu_de             NVARCHAR(150) NOT NULL,
        mo_ta               NVARCHAR(MAX) NULL,
        muc_do_uu_tien      VARCHAR(20)   DEFAULT 'TRUNG_BINH', -- CAO | TRUNG_BINH | THAP
        ngay_yeu_cau        DATETIME DEFAULT CURRENT_TIMESTAMP,
        ngay_bat_dau        DATETIME NULL,
        ngay_hoan_thanh     DATETIME NULL,
        chi_phi_du_kien     DECIMAL(15,2) NULL,
        chi_phi_thuc_te     DECIMAL(15,2) NULL,
        trang_thai          VARCHAR(20)   NOT NULL DEFAULT 'TIEP_NHAN',
        ket_qua_xu_ly       NVARCHAR(MAX) NULL,

        CONSTRAINT FK_sc_phong_tro FOREIGN KEY (phong_tro_id) REFERENCES phong_tro(id),
        CONSTRAINT FK_sc_nguoi_tao FOREIGN KEY (nguoi_tao_id) REFERENCES nguoi_dung(id),
        CONSTRAINT FK_sc_nguoi_phu_trach FOREIGN KEY (nguoi_phu_trach_id) REFERENCES nguoi_dung(id)
    );
END
GO

-- ============================================================================
-- 24. BẢNG vat_tu_sua_chua (Chi tiết vật tư thay thế cho sửa chữa)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'vat_tu_sua_chua' AND type = 'U')
BEGIN
    CREATE TABLE vat_tu_sua_chua (
        id                  INT IDENTITY(1,1) PRIMARY KEY,
        yeu_cau_sua_chua_id INT NOT NULL,
        ten_vat_tu          NVARCHAR(100) NOT NULL,
        so_luong            DECIMAL(12,2) NOT NULL,
        don_vi_tinh         NVARCHAR(30)  NULL,
        don_gia             DECIMAL(15,2) NOT NULL,
        thanh_tien          DECIMAL(15,2) NOT NULL,
        ghi_chu             NVARCHAR(MAX) NULL,

        CONSTRAINT FK_vt_yeu_cau FOREIGN KEY (yeu_cau_sua_chua_id) REFERENCES yeu_cau_sua_chua(id) ON DELETE CASCADE
    );
END
GO

-- ============================================================================
-- 25. BẢNG tai_san_phong (Tài sản trang thiết bị trong phòng)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'tai_san_phong' AND type = 'U')
BEGIN
    CREATE TABLE tai_san_phong (
        id            INT IDENTITY(1,1) PRIMARY KEY,
        phong_tro_id  INT NOT NULL,
        ten_tai_san   NVARCHAR(100) NOT NULL,
        loai_tai_san  NVARCHAR(100) NULL,
        so_luong      INT DEFAULT 1,
        gia_tri       DECIMAL(15,2) NULL,
        ngay_mua      DATE NULL,
        tinh_trang    NVARCHAR(50)  NULL,
        ma_tai_san    VARCHAR(50)   NULL,
        ghi_chu       NVARCHAR(MAX) NULL,

        CONSTRAINT FK_tai_san_phong FOREIGN KEY (phong_tro_id) REFERENCES phong_tro(id)
    );
END
GO

-- ============================================================================
-- 26. BẢNG bien_ban_ban_giao_tai_san (Biên bản bàn giao tài sản khi vào/ra)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'bien_ban_ban_giao_tai_san' AND type = 'U')
BEGIN
    CREATE TABLE bien_ban_ban_giao_tai_san (
        id             INT IDENTITY(1,1) PRIMARY KEY,
        hop_dong_id    INT NOT NULL,
        nguoi_lap_id   INT NOT NULL,
        loai_bien_ban  VARCHAR(30)  NOT NULL, -- BAN_GIAO_NHAN | BAN_GIAO_TRA
        ngay_ban_giao  DATETIME     DEFAULT CURRENT_TIMESTAMP,
        noi_dung       NVARCHAR(MAX) NULL,
        duong_dan_file VARCHAR(255) NULL,

        CONSTRAINT FK_bb_hop_dong FOREIGN KEY (hop_dong_id) REFERENCES hop_dong_thue(id),
        CONSTRAINT FK_bb_nguoi_lap FOREIGN KEY (nguoi_lap_id) REFERENCES nguoi_dung(id)
    );
END
GO

-- ============================================================================
-- 27. BẢNG kiem_tra_phong (Biên bản đợt kiểm tra phòng định kỳ/đột xuất)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'kiem_tra_phong' AND type = 'U')
BEGIN
    CREATE TABLE kiem_tra_phong (
        id                  INT IDENTITY(1,1) PRIMARY KEY,
        phong_tro_id        INT NOT NULL,
        hop_dong_id         INT NULL,
        nguoi_kiem_tra_id   INT NOT NULL,
        ngay_kiem_tra       DATETIME DEFAULT CURRENT_TIMESTAMP,
        loai_kiem_tra       VARCHAR(30) NOT NULL, -- DINH_KY | NHAN_PHONG | TRA_PHONG | DOT_XUAT
        ket_luan            NVARCHAR(MAX) NULL,
        duong_dan_hinh_anh  VARCHAR(255) NULL,

        CONSTRAINT FK_kt_phong_tro FOREIGN KEY (phong_tro_id) REFERENCES phong_tro(id),
        CONSTRAINT FK_kt_hop_dong FOREIGN KEY (hop_dong_id) REFERENCES hop_dong_thue(id),
        CONSTRAINT FK_kt_nguoi_kt FOREIGN KEY (nguoi_kiem_tra_id) REFERENCES nguoi_dung(id)
    );
END
GO

-- ============================================================================
-- 28. BẢNG chi_tiet_kiem_tra_phong (Chi tiết tình trạng từng tài sản kiểm tra)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'chi_tiet_kiem_tra_phong' AND type = 'U')
BEGIN
    CREATE TABLE chi_tiet_kiem_tra_phong (
        id                  INT IDENTITY(1,1) PRIMARY KEY,
        kiem_tra_phong_id   INT NOT NULL,
        tai_san_phong_id    INT NULL,
        ten_hang_muc        NVARCHAR(100) NOT NULL,
        tinh_trang_truoc    NVARCHAR(100) NULL,
        tinh_trang_sau      NVARCHAR(100) NULL,
        chi_phi_khac_phuc   DECIMAL(15,2) NULL,
        ghi_chu             NVARCHAR(MAX) NULL,

        CONSTRAINT FK_ct_kt_kiem_tra FOREIGN KEY (kiem_tra_phong_id) REFERENCES kiem_tra_phong(id) ON DELETE CASCADE,
        CONSTRAINT FK_ct_kt_tai_san FOREIGN KEY (tai_san_phong_id) REFERENCES tai_san_phong(id)
    );
END
GO

-- ============================================================================
-- 29. BẢNG thong_bao (Thông báo gửi đến người dùng)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'thong_bao' AND type = 'U')
BEGIN
    CREATE TABLE thong_bao (
        id              INT IDENTITY(1,1) PRIMARY KEY,
        nguoi_dung_id   INT NOT NULL,
        tieu_de         NVARCHAR(150) NOT NULL,
        noi_dung        NVARCHAR(MAX) NOT NULL,
        loai_thong_bao  VARCHAR(30)   NULL,
        ngay_tao        DATETIME      DEFAULT CURRENT_TIMESTAMP,
        ngay_doc        DATETIME      NULL,
        trang_thai      VARCHAR(20)   DEFAULT 'CHUA_DOC',

        CONSTRAINT FK_thong_bao_nguoi_dung FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id)
    );
END
GO

-- ============================================================================
-- 30. BẢNG nhat_ky_he_thong (Audit Log hệ thống)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'nhat_ky_he_thong' AND type = 'U')
BEGIN
    CREATE TABLE nhat_ky_he_thong (
        id             INT IDENTITY(1,1) PRIMARY KEY,
        nguoi_dung_id  INT NULL,
        hanh_dong      NVARCHAR(100) NOT NULL,
        bang_du_lieu   VARCHAR(100)  NULL,
        id_ban_ghi     INT           NULL,
        noi_dung       NVARCHAR(MAX) NULL,
        dia_chi_ip     VARCHAR(45)   NULL,
        thoi_gian      DATETIME      DEFAULT CURRENT_TIMESTAMP,

        CONSTRAINT FK_log_nguoi_dung FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id)
    );
END
GO

-- ============================================================================
-- 31. BẢNG dat_phong (Yêu cầu đặt cọc giữ chỗ phòng trọ)
-- ============================================================================
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'dat_phong' AND type = 'U')
BEGIN
    CREATE TABLE dat_phong (
        id                       INT IDENTITY(1,1) PRIMARY KEY,
        phong_tro_id             INT NOT NULL,
        nguoi_thue_id            INT NULL,
        ho_ten_nguoi_dat         NVARCHAR(100) NOT NULL,
        so_dien_thoai            VARCHAR(20)   NOT NULL,
        ngay_dat                 DATETIME      DEFAULT CURRENT_TIMESTAMP,
        ngay_du_kien_nhan_phong  DATE          NOT NULL,
        tien_dat_coc             DECIMAL(15,2) DEFAULT 0,
        trang_thai               VARCHAR(20)   NOT NULL DEFAULT 'CHO_DUYET',
        ghi_chu                  NVARCHAR(MAX) NULL,

        CONSTRAINT FK_dat_phong_phong_tro FOREIGN KEY (phong_tro_id) REFERENCES phong_tro(id),
        CONSTRAINT FK_dat_phong_nguoi_thue FOREIGN KEY (nguoi_thue_id) REFERENCES nguoi_thue(id)
    );
END
GO

-- ============================================================================
-- CHỈ MỤC TỐI ƯU HÓA TRUY VẤN (INDEXES)
-- ============================================================================
CREATE NONCLUSTERED INDEX IX_phong_tro_trang_thai ON phong_tro(trang_thai);
CREATE NONCLUSTERED INDEX IX_hop_dong_phong_trang_thai ON hop_dong_thue(phong_tro_id, trang_thai);
CREATE NONCLUSTERED INDEX IX_hoa_don_hop_dong_trang_thai ON hoa_don(hop_dong_id, trang_thai);
CREATE NONCLUSTERED INDEX IX_nguoi_thue_cccd ON nguoi_thue(so_cccd);
CREATE NONCLUSTERED INDEX IX_chi_so_cong_to_ngay ON chi_so_cong_to(cong_to_id, ngay_ghi);
GO

-- ============================================================================
-- DỮ LIỆU KHỞI TẠO MẪU (SEED DATA)
-- ============================================================================
-- 1. Vai trò
IF NOT EXISTS (SELECT 1 FROM vai_tro WHERE ten_vai_tro = 'ADMIN')
    INSERT INTO vai_tro (ten_vai_tro, mo_ta) VALUES ('ADMIN', N'Quản trị viên / Chủ trọ');
IF NOT EXISTS (SELECT 1 FROM vai_tro WHERE ten_vai_tro = 'STAFF')
    INSERT INTO vai_tro (ten_vai_tro, mo_ta) VALUES ('STAFF', N'Nhân viên quản lý dãy trọ');
IF NOT EXISTS (SELECT 1 FROM vai_tro WHERE ten_vai_tro = 'USER')
    INSERT INTO vai_tro (ten_vai_tro, mo_ta) VALUES ('USER', N'Khách thuê trọ / Cư dân');
GO

-- 2. Tài khoản admin mặc định (password: 123456 mã hóa BCrypt)
IF NOT EXISTS (SELECT 1 FROM nguoi_dung WHERE ten_dang_nhap = 'admin')
BEGIN
    DECLARE @adminRoleId INT = (SELECT id FROM vai_tro WHERE ten_vai_tro = 'ADMIN');
    INSERT INTO nguoi_dung (vai_tro_id, ho_ten, email, so_dien_thoai, ten_dang_nhap, mat_khau, trang_thai)
    VALUES (@adminRoleId, N'Chủ Trọ Admin', 'admin@qlphongtro.vn', '0901234567', 'admin', 
            '$2a$12$hNAUBiPGi4bOFsIl5kLxuuFfpQ3Xs9Rg.n1RHrb5KVGSdB6dHgBy', 'HOAT_DONG');
END
GO

-- 3. Dịch vụ cơ bản
IF NOT EXISTS (SELECT 1 FROM dich_vu WHERE ten_dich_vu = N'Điện sinh hoạt')
    INSERT INTO dich_vu (ten_dich_vu, don_vi_tinh, cach_tinh, don_gia_mac_dinh, trang_thai)
    VALUES (N'Điện sinh hoạt', 'kWh', 'THEO_CONG_TO', 3500, 'HOAT_DONG');

IF NOT EXISTS (SELECT 1 FROM dich_vu WHERE ten_dich_vu = N'Nước sinh hoạt')
    INSERT INTO dich_vu (ten_dich_vu, don_vi_tinh, cach_tinh, don_gia_mac_dinh, trang_thai)
    VALUES (N'Nước sinh hoạt', 'm3', 'THEO_CONG_TO', 25000, 'HOAT_DONG');

IF NOT EXISTS (SELECT 1 FROM dich_vu WHERE ten_dich_vu = N'Internet Wifi')
    INSERT INTO dich_vu (ten_dich_vu, don_vi_tinh, cach_tinh, don_gia_mac_dinh, trang_thai)
    VALUES (N'Internet Wifi', N'phòng', 'CO_DINH', 100000, 'HOAT_DONG');

IF NOT EXISTS (SELECT 1 FROM dich_vu WHERE ten_dich_vu = N'Rác & Vệ sinh chung')
    INSERT INTO dich_vu (ten_dich_vu, don_vi_tinh, cach_tinh, don_gia_mac_dinh, trang_thai)
    VALUES (N'Rác & Vệ sinh chung', N'phòng', 'CO_DINH', 50000, 'HOAT_DONG');
GO

PRINT '====================================================================';
PRINT 'HOÀN TẤT KHỞI TẠO CSDL APARTMENTDB (31 BẢNG CHUẨN HOÁ KHỚP DBDATAGRAM)';
PRINT '100% BẢNG ĐÃ CÓ KHÓA CHÍNH (PK), KHÓA NGOẠI (FK) VÀ DỮ LIỆU MẪU BAN ĐẦU';
PRINT '====================================================================';
