package com.qlphongtro;

import com.qlphongtro.entity.RoomListing;
import com.qlphongtro.repository.BookingRequestRepository;
import com.qlphongtro.repository.RoomListingRepository;
import com.qlphongtro.service.impl.RoomListingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm thử RoomListingService - Nghiệp vụ phòng trọ")
class RoomListingServiceTest {

    @Mock
    private RoomListingRepository roomListingRepository;

    @Mock
    private BookingRequestRepository bookingRequestRepository;

    @Mock
    private com.qlphongtro.config.DatabaseUnicodeMigrator databaseUnicodeMigrator;

    @Mock
    private Environment environment;

    private RoomListingServiceImpl roomListingService;

    private List<RoomListing> sampleRooms;

    @BeforeEach
    void setUp() {
        roomListingService = new RoomListingServiceImpl(
                roomListingRepository, bookingRequestRepository, databaseUnicodeMigrator, environment, false);
        sampleRooms = new ArrayList<>();
        // Tạo 14 phòng mẫu tương tự trong hệ thống
        for (int i = 1; i <= 14; i++) {
            RoomListing r = new RoomListing();
            r.setListingId(i);
            r.setRoomCode("P" + (100 + i));
            r.setPrice(BigDecimal.valueOf(2_000_000 + i * 200_000L));
            r.setArea(BigDecimal.valueOf(15 + i * 2.0));
            r.setFloorNo((i % 5) + 1);
            if (i <= 8) {
                r.setStatus("TRONG");
            } else if (i <= 12) {
                r.setStatus("DA_THUE");
            } else {
                r.setStatus("BAO_TRI");
            }
            sampleRooms.add(r);
        }
    }

    @Test
    @DisplayName("Không seed phòng demo khi không bật cờ và không ở profile dev")
    void initDoesNotSeedDemoRoomsByDefault() {
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
        roomListingService.init();

        verify(databaseUnicodeMigrator).migrateIfNeeded();
        verify(roomListingRepository, never()).save(any(RoomListing.class));
    }

    @Test
    @DisplayName("Có thể bật seed phòng demo bằng cờ cấu hình")
    void initSeedsDemoRoomsWhenExplicitlyEnabled() {
        ReflectionTestUtils.setField(roomListingService, "demoRoomSeedEnabled", true);
        when(roomListingRepository.count()).thenReturn(0L);

        roomListingService.init();

        verify(roomListingRepository, times(14)).save(any(RoomListing.class));
    }

    @Test
    @DisplayName("Lấy danh sách tất cả phòng - trả về đủ 14 phòng")
    void testGetAllRooms() {
        when(roomListingRepository.findAll(any(Sort.class))).thenReturn(sampleRooms);

        List<RoomListing> result = roomListingService.getAllRooms();

        assertNotNull(result);
        assertEquals(14, result.size());
        verify(roomListingRepository, times(1)).findAll(any(Sort.class));
    }

    @Test
    @DisplayName("Lấy danh sách phòng còn trống - trả về đúng 8 phòng")
    void testGetAvailableRooms() {
        List<RoomListing> availableRooms = sampleRooms.stream().filter(r -> "TRONG".equals(r.getStatus())).toList();
        when(roomListingRepository.findByStatus("TRONG")).thenReturn(availableRooms);

        List<RoomListing> result = roomListingService.getAvailableRooms();

        assertNotNull(result);
        assertEquals(8, result.size());
        assertTrue(result.stream().allMatch(r -> "TRONG".equals(r.getStatus())));
    }

    @Test
    @DisplayName("Lấy phòng theo ID hợp lệ - trả về đúng phòng")
    void testGetById_Success() {
        RoomListing expected = sampleRooms.get(0);
        when(roomListingRepository.findById(1)).thenReturn(Optional.of(expected));

        RoomListing result = roomListingService.getById(1);

        assertNotNull(result);
        assertEquals("P101", result.getRoomCode());
        assertEquals(1, result.getListingId());
    }

    @Test
    @DisplayName("Lấy phòng theo ID không tồn tại - ném ra IllegalArgumentException")
    void testGetById_NotFound() {
        when(roomListingRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> roomListingService.getById(999));
    }

    @Test
    @DisplayName("Không tự hiển thị tiện nghi chưa được lưu")
    void missingAmenitiesAreNotInvented() {
        RoomListing room = new RoomListing();
        assertTrue(room.getAmenityList().isEmpty());
    }

    @Test
    @DisplayName("Lọc theo trạng thái TRONG trả về đúng số phòng còn trống (8 phòng)")
    void testFilterRooms_ByStatusTrong() {
        List<RoomListing> trongRooms = sampleRooms.stream().filter(r -> "TRONG".equals(r.getStatus())).toList();
        when(roomListingRepository.findAll(any(Sort.class))).thenReturn(sampleRooms);

        List<RoomListing> result = roomListingService.filterRooms("", "TRONG", "all", null, "price_asc");

        assertNotNull(result);
        assertEquals(8, result.size());
        assertTrue(result.stream().allMatch(r -> "TRONG".equals(r.getStatus())));
    }

    @Test
    @DisplayName("Lọc không truyền điều kiện - trả về toàn bộ 14 phòng")
    void testFilterRooms_NoFilter() {
        when(roomListingRepository.findAll(any(Sort.class))).thenReturn(sampleRooms);

        List<RoomListing> result = roomListingService.filterRooms("", "ALL", "all", null, "price_asc");

        assertNotNull(result);
        assertEquals(14, result.size());
    }

    @Test
    @DisplayName("Không xóa bài đăng đã có yêu cầu thuê")
    void testDeleteRoomWithBookingRequest_IsRejected() {
        RoomListing room = sampleRooms.get(0);
        when(roomListingRepository.findById(room.getListingId())).thenReturn(Optional.of(room));
        when(bookingRequestRepository.existsByRoom_ListingId(room.getListingId())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> roomListingService.delete(room.getListingId()));

        verify(roomListingRepository, never()).delete(any(RoomListing.class));
    }
}
