package com.qlphongtro;

import com.qlphongtro.entity.RoomListing;
import com.qlphongtro.repository.RoomListingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "REMEMBER_ME_KEY=test-only-remember-me-key",
        "spring.datasource.url=jdbc:h2:mem:unicodeDb;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration/common,classpath:db/migration/{vendor}"
})
class VietnameseUnicodeSupportIntegrationTest {

    @Autowired
    private RoomListingRepository roomListingRepository;

    @Test
    @Transactional
    @DisplayName("Lưu và đọc chuỗi tiếng Việt có dấu đầy đủ (ạ ọ ầ ấ ệ ờ ỗ ể đ Đ) không bị biến thành dấu ?")
    void shouldPersistAndRetrieveVietnameseUnicodeAccentsWithoutCorruption() {
        String fullVietnameseAccents = "Phòng trọ cao cấp số 101 - Đầy đủ tiện nghi: máy giặt, điều hòa, tủ lạnh, gác lửng, giờ giấc tự do, gần đại học (ạ ọ ầ ấ ệ ờ ỗ ể đ Đ)";
        String vietnameseAmenities = "Nóng lạnh, điều hòa, tủ lạnh, chỗ để xe, an ninh tốt, tự do giờ giấc";

        RoomListing listing = new RoomListing();
        listing.setRoomCode("VN_TEST_101");
        listing.setTitle(fullVietnameseAccents);
        listing.setAmenities(vietnameseAmenities);
        listing.setPrice(BigDecimal.valueOf(3500000));
        listing.setArea(BigDecimal.valueOf(25.5));
        listing.setStatus("AVAILABLE");

        RoomListing saved = roomListingRepository.save(listing);
        assertThat(saved.getListingId()).isNotNull();

        Optional<RoomListing> retrieved = roomListingRepository.findById(saved.getListingId());
        assertThat(retrieved).isPresent();

        RoomListing found = retrieved.get();
        // Kiểm tra chính xác từng ký tự có dấu, tuyệt đối không bị thành dấu ?
        assertThat(found.getTitle()).isEqualTo(fullVietnameseAccents);
        assertThat(found.getTitle()).doesNotContain("?");
        assertThat(found.getAmenities()).isEqualTo(vietnameseAmenities);
        assertThat(found.getAmenities()).doesNotContain("?");
    }
}
