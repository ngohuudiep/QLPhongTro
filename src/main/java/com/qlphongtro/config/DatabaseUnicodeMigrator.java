package com.qlphongtro.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Tự động kiểm tra và chuẩn hóa kiểu dữ liệu NVARCHAR trong SQL Server khi ứng dụng khởi động.
 * Đảm bảo hệ thống hoạt động trơn tru ngay cả khi người dùng chưa chạy script bằng tay.
 */
@Component
public class DatabaseUnicodeMigrator {

    private static final Logger log = LoggerFactory.getLogger(DatabaseUnicodeMigrator.class);

    private final DataSource dataSource;

    @Autowired
    public DatabaseUnicodeMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public synchronized void migrateIfNeeded() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            log.info("[DB MIGRATION] Bắt đầu kiểm tra schema SQL Server cho ApartmentDB...");

            // 1. Kiểm tra và ALTER bảng RoomListings
            boolean needAlterTitle = false;
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'RoomListings' AND COLUMN_NAME = 'title'")) {
                if (rs.next()) {
                    String type = rs.getString(1);
                    if ("varchar".equalsIgnoreCase(type) || "char".equalsIgnoreCase(type)) {
                        needAlterTitle = true;
                    }
                }
            } catch (Exception e) {
                log.warn("[DB MIGRATION] Không thể kiểm tra kiểu cột RoomListings.title: {}", e.getMessage());
            }

            if (needAlterTitle) {
                log.info("[DB MIGRATION] Phát hiện các cột RoomListings đang là VARCHAR. Đang tự động ALTER sang NVARCHAR...");
                executeQuietly(stmt, "ALTER TABLE RoomListings ALTER COLUMN title NVARCHAR(255) NOT NULL");
                executeQuietly(stmt, "ALTER TABLE RoomListings ALTER COLUMN description NVARCHAR(MAX)");
                executeQuietly(stmt, "ALTER TABLE RoomListings ALTER COLUMN amenities NVARCHAR(500)");
                executeQuietly(stmt, "ALTER TABLE RoomListings ALTER COLUMN current_tenant NVARCHAR(150)");
                executeQuietly(stmt, "ALTER TABLE RoomListings ALTER COLUMN image_gallery NVARCHAR(MAX)");
                log.info("[DB MIGRATION] Hoàn thành ALTER các cột RoomListings sang NVARCHAR.");
            }

            // 2. Kiểm tra và ALTER bảng Users (full_name)
            executeQuietly(stmt, "ALTER TABLE Users ALTER COLUMN full_name NVARCHAR(150)");

            // 3. Kiểm tra và ALTER bảng BookingRequests (note)
            executeQuietly(stmt, "ALTER TABLE BookingRequests ALTER COLUMN note NVARCHAR(500)");

            // 4. Kiểm tra xem dữ liệu trong RoomListings có bị dấu '?' hoặc rỗng hoặc lệch ID không
            boolean hasCorruptedData = false;
            int roomCount = 0;
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM RoomListings")) {
                if (rs.next()) {
                    roomCount = rs.getInt(1);
                }
            } catch (Exception ignored) {}

            if (roomCount > 0) {
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM RoomListings WHERE title LIKE '%?%' OR amenities LIKE '%?%'")) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        hasCorruptedData = true;
                    }
                } catch (Exception ignored) {}
            }

            // Nếu bảng rỗng, hoặc chứa ký tự '?' do seed cũ bằng VARCHAR, hoặc không đủ 14 phòng
            if (hasCorruptedData || roomCount != 14) {
                log.info("[DB MIGRATION] Dữ liệu RoomListings hiện có: {} phòng (có dấu '?' hoặc chưa đủ 14). Tiến hành làm sạch và reset IDENTITY...", roomCount);
                executeQuietly(stmt, "DELETE FROM BookingRequests");
                executeQuietly(stmt, "DELETE FROM RoomListings");
                executeQuietly(stmt, "DBCC CHECKIDENT ('RoomListings', RESEED, 0)");
                log.info("[DB MIGRATION] Đã reset IDENTITY ('RoomListings') về 0. Sẵn sàng cho seed 14 phòng chuẩn.");
            } else {
                log.info("[DB MIGRATION] Dữ liệu RoomListings đã có đủ {} phòng chuẩn.", roomCount);
            }

        } catch (Exception e) {
            log.error("[DB MIGRATION] Lỗi kết nối khi chạy auto-migration: {}", e.getMessage());
        }
    }

    private void executeQuietly(Statement stmt, String sql) {
        try {
            stmt.execute(sql);
        } catch (Exception e) {
            log.debug("[DB MIGRATION] SQL execute notice: {} -> {}", sql, e.getMessage());
        }
    }
}
