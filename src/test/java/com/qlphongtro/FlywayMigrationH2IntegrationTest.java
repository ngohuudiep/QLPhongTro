package com.qlphongtro;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử tích hợp Flyway Migration + Hibernate ddl-auto=validate trên H2
 * in-memory:
 * 1. Đảm bảo Flyway tự động chạy tuần tự các migration từ V1 đến V2_7 trên DB
 * trắng.
 * 2. Đảm bảo Hibernate ddl-auto=validate xác nhận toàn bộ 18 JPA Entities khớp
 * hoàn toàn với schema của Flyway.
 * 3. Kiểm tra danh mục các phiên bản migration đã áp dụng thành công.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "REMEMBER_ME_KEY=test-only-remember-me-key",
        "spring.datasource.url=jdbc:h2:mem:flywayValidateDb;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration/common,classpath:db/migration/{vendor}"
})
class FlywayMigrationH2IntegrationTest {

    @Autowired
    private Flyway flyway;

    @Test
    @DisplayName("Flyway chạy thành công các migration từ V1 đến V2_8 và Hibernate validate khớp toàn bộ Entity")
    void flywayAppliesAllMigrationsAndHibernateValidatesSchema() {
        assertThat(flyway).isNotNull();

        MigrationInfo[] applied = flyway.info().applied();
        assertThat(applied).isNotEmpty();

        List<String> versions = Arrays.stream(applied)
                .map(info -> info.getVersion().getVersion())
                .toList();

        // Xác nhận đã áp dụng đầy đủ 17 migration (1, 2.1 - 2.16)
        assertThat(versions).containsExactly("1", "2.1", "2.2", "2.3", "2.4", "2.5", "2.6", "2.7", "2.8", "2.9", "2.10", "2.11", "2.12", "2.13", "2.14", "2.15", "2.16");

        // Xác nhận trạng thái của từng migration đều thành công
        for (MigrationInfo info : applied) {
            assertThat(info.getState().isApplied())
                    .withFailMessage("Migration %s chưa ở trạng thái áp dụng thành công", info.getScript())
                    .isTrue();
        }
    }
}
