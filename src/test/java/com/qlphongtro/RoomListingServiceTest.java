package com.qlphongtro;

import com.qlphongtro.entity.RoomListing;
import com.qlphongtro.repository.RoomListingRepository;
import com.qlphongtro.service.impl.RoomListingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomListingServiceTest {

    @Mock
    private RoomListingRepository roomListingRepository;

    @Mock
    private com.qlphongtro.config.DatabaseUnicodeMigrator databaseUnicodeMigrator;

    @InjectMocks
    private RoomListingServiceImpl roomListingService;

    private List<RoomListing> sampleRooms;

    @BeforeEach
    void setUp() {
        sampleRooms = new ArrayList<>();
        // Tạo 14 phòng mẫu tương tự trong hệ thống
        for (int i = 1; i <= 14; i++) {
            RoomListing r = new RoomListing();
            r.setListingId(i);
            r.setRoomCode("P" + (100 + i));
            r.setTitle("Phòng trọ mẫu số " + i);
            r.setPrice(new BigDecimal("3000000"));
            r.setArea(new BigDecimal("25.0"));
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
    @DisplayName("Kiểm tra getAllRooms trả về đầy đủ 14 phòng và không gọi deleteAll")
    void testGetAllRooms_ReturnsAllRooms() {
        when(roomListingRepository.findAll(any(Sort.class))).thenReturn(sampleRooms);

        List<RoomListing> result = roomListingService.getAllRooms();

        assertNotNull(result);
        assertEquals(14, result.size());
        verify(roomListingRepository, never()).deleteAll();
    }

    @Test
    @DisplayName("Tìm phòng theo ID hợp lệ (1..14) trả về đúng phòng")
    void testGetById_ValidId_ReturnsRoom() {
        RoomListing expectedRoom = sampleRooms.get(0); // ID 1, P101
        when(roomListingRepository.findById(1)).thenReturn(Optional.of(expectedRoom));

        RoomListing actualRoom = roomListingService.getById(1);

        assertNotNull(actualRoom);
        assertEquals(1, actualRoom.getListingId());
        assertEquals("P101", actualRoom.getRoomCode());
        verify(roomListingRepository, never()).deleteAll();
    }

    @Test
    @DisplayName("Tìm phòng theo ID không tồn tại (ví dụ 364 hoặc 999) ném IllegalArgumentException thân thiện")
    void testGetById_NotFound_ThrowsException() {
        when(roomListingRepository.findById(364)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            roomListingService.getById(364);
        });

        assertTrue(ex.getMessage().contains("Không tìm thấy thông tin phòng ID: 364"));
        verify(roomListingRepository, never()).deleteAll();
    }

    @Test
    @DisplayName("Lọc theo trạng thái TRONG trả về đúng số phòng còn trống (8 phòng)")
    void testFilterRooms_ByStatusTrong() {
        List<RoomListing> trongRooms = sampleRooms.stream().filter(r -> "TRONG".equals(r.getStatus())).toList();
        when(roomListingRepository.findByStatus(eq("TRONG"), any(Sort.class))).thenReturn(trongRooms);

        List<RoomListing> result = roomListingService.filterRooms("", "TRONG", "all", null, "newest");

        assertNotNull(result);
        assertEquals(8, result.size());
    }

    @Test
    @DisplayName("Lọc khi không có điều kiện nào trả về toàn bộ 14 phòng")
    void testFilterRooms_All() {
        when(roomListingRepository.findAll(any(Sort.class))).thenReturn(sampleRooms);

        List<RoomListing> result = roomListingService.filterRooms("", "ALL", "all", null, "newest");

        assertNotNull(result);
        assertEquals(14, result.size());
    }
}
