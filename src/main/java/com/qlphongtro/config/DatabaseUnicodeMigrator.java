package com.qlphongtro.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private final boolean enabled;

    @Autowired
    public DatabaseUnicodeMigrator(
            DataSource dataSource,
            @Value("${app.db.unicode-migration.enabled:false}") boolean enabled) {
        this.dataSource = dataSource;
        this.enabled = enabled;
    }

    public synchronized void migrateIfNeeded() {
        if (!enabled) {
            log.info("[DB MIGRATION] Unicode schema migration disabled.");
            return;
        }

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
