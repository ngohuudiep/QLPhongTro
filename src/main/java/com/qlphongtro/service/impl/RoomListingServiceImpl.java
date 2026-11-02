package com.qlphongtro.service.impl;

import com.qlphongtro.entity.RoomListing;
import com.qlphongtro.repository.RoomListingRepository;
import com.qlphongtro.service.RoomListingService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RoomListingServiceImpl implements RoomListingService {

    private final RoomListingRepository roomListingRepository;
    private final com.qlphongtro.config.DatabaseUnicodeMigrator databaseUnicodeMigrator;

    @Autowired
    public RoomListingServiceImpl(RoomListingRepository roomListingRepository,
                                  com.qlphongtro.config.DatabaseUnicodeMigrator databaseUnicodeMigrator) {
        this.roomListingRepository = roomListingRepository;
        this.databaseUnicodeMigrator = databaseUnicodeMigrator;
    }

    @PostConstruct
    public void init() {
        databaseUnicodeMigrator.migrateIfNeeded();
        seedDemoRoomsIfEmpty();
    }

    @Override
    public List<RoomListing> getAllRooms() {
        return roomListingRepository.findAll(Sort.by(Sort.Direction.ASC, "listingId"));
    }

    @Override
    public List<RoomListing> getAvailableRooms() {
        return roomListingRepository.findByStatusOrderByPriceAsc("TRONG");
    }

    @Override
    public List<RoomListing> getAvailableRoomsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return roomListingRepository.searchAndFilter(null, "TRONG", minPrice, maxPrice, null, Sort.by(Sort.Direction.ASC, "price"));
    }

    @Override
    public List<RoomListing> searchRooms(String keyword) {
        return roomListingRepository.searchAndFilter(keyword, null, null, null, null, Sort.by(Sort.Direction.ASC, "price"));
    }

    @Override
    public List<RoomListing> filterRooms(String keyword, String status, String priceRange, Integer floorNo, String sortBy) {
        // 1. Phân tích khoảng giá
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        if (priceRange != null && !priceRange.equalsIgnoreCase("all") && !priceRange.trim().isEmpty()) {
            switch (priceRange) {
                case "under2m" -> {
                    minPrice = BigDecimal.ZERO;
                    maxPrice = new BigDecimal("2500000");
                }
                case "2to4m" -> {
                    minPrice = new BigDecimal("2500000");
                    maxPrice = new BigDecimal("4500000");
                }
                case "over4m" -> {
                    minPrice = new BigDecimal("4500000");
                    maxPrice = new BigDecimal("999999999");
                }
            }
        }

        // 2. Phân tích sắp xếp
        Sort sort = Sort.by(Sort.Direction.ASC, "listingId");
        if ("price_asc".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "price");
        } else if ("price_desc".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "price");
        } else if ("area_desc".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "area");
        } else if ("newest".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        // 3. Chuẩn hóa bộ lọc
        String kw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String st = (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) ? status.trim().toUpperCase() : null;

        // Nếu không có bất kỳ bộ lọc nào -> trả về tất cả phòng
        if (kw == null && st == null && minPrice == null && maxPrice == null && floorNo == null) {
            return roomListingRepository.findAll(sort);
        }

        // Nếu chỉ lọc theo trạng thái (chính là trường hợp phổ biến ở trang quản lý admin)
        if (kw == null && st != null && minPrice == null && maxPrice == null && floorNo == null) {
            return roomListingRepository.findByStatus(st, sort);
        }

        return roomListingRepository.searchAndFilter(kw, st, minPrice, maxPrice, floorNo, sort);
    }

    @Override
    public RoomListing getById(Integer id) {
        return roomListingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin phòng ID: " + id));
    }

    @Override
    public long countAvailableRooms() {
        return roomListingRepository.countByStatus("TRONG");
    }

    @Override
    @Transactional
    public RoomListing save(RoomListing room) {
        if (room.getPrice() == null || room.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá thuê phải lớn hơn 0 đồng!");
        }

        if (room.getArea() == null || room.getArea().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Diện tích phòng phải lớn hơn 0 m²!");
        }

        if (isRoomCodeTaken(room.getRoomCode(), room.getListingId())) {
            throw new IllegalArgumentException("Mã phòng '" + room.getRoomCode() + "' đã tồn tại trong hệ thống!");
        }

        if (room.getListingId() == null && room.getCreatedAt() == null) {
            room.setCreatedAt(LocalDateTime.now());
        }

        return roomListingRepository.save(room);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        if (!roomListingRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy phòng để xóa với ID: " + id);
        }
        roomListingRepository.deleteById(id);
    }

    @Override
    public boolean isRoomCodeTaken(String roomCode, Integer excludeId) {
        return roomListingRepository.findByRoomCode(roomCode.trim())
                .map(r -> !r.getListingId().equals(excludeId))
                .orElse(false);
    }

    @Override
    @Transactional
    public synchronized void seedDemoRoomsIfEmpty() {
        if (roomListingRepository.count() > 0) {
            return; // Đã có dữ liệu trong DB, giữ nguyên dữ liệu và khóa chính ID ổn định
        }

        List<RoomListing> demoRooms = new ArrayList<>();

        demoRooms.add(new RoomListing(null, "P101", "Phòng Studio Ban công thoáng mát, Full nội thất",
                "Phòng đầy đủ nội thất cao cấp: máy lạnh Inverter, tủ lạnh 2 cánh, giường nệm cao su, tủ quần áo âm tường. Ban công đón ánh sáng tự nhiên.",
                1, new BigDecimal("28.5"), new BigDecimal("3800000"), "TRONG",
                "Điều hòa, Wifi, Nóng lạnh, Gác lửng, Tủ lạnh, Ban công, Khóa vân tay",
                "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80",
                null, null, null, null));

        demoRooms.add(new RoomListing(null, "P102", "Phòng trọ gác lửng đúc cao cấp, giờ giấc tự do",
                "Gác lửng đúc cao 2m đứng không đụng đầu, cầu thang tay vịn gỗ an toàn. Không chung chủ, cổng khóa vân tay 24/7.",
                1, new BigDecimal("25.0"), new BigDecimal("3200000"), "TRONG",
                "Điều hòa, Wifi, Nóng lạnh, Gác lửng, Chỗ để xe",
                "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80",
                null, null, null, null));

        demoRooms.add(new RoomListing(null, "P103", "Phòng trọ giá sinh viên gần các trường đại học",
                "Phòng sạch sẽ, gạch ốp tường 100%, có cửa sổ trời thoáng mát. Cách trạm xe buýt và chợ chỉ 200m.",
                1, new BigDecimal("18.0"), new BigDecimal("2200000"), "DA_THUE",
                "Wifi, Nóng lạnh, Chỗ để xe",
                "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80",
                "Trần Hoàng Nam", "0912345678", LocalDate.of(2026, 1, 15), LocalDate.of(2027, 1, 15)));

        demoRooms.add(new RoomListing(null, "P201", "Căn hộ mini 1 phòng ngủ phong cách Bắc Âu Scandinavian",
                "Căn hộ thiết kế hiện đại tông màu trắng kem và gỗ sồi. Bếp riêng biệt có máy hút mùi, sofa êm ái.",
                2, new BigDecimal("35.0"), new BigDecimal("4900000"), "TRONG",
                "Điều hòa, Wifi, Nóng lạnh, Tủ lạnh, Máy giặt, Bếp riêng, Sofa, Khóa vân tay",
                "https://images.unsplash.com/photo-1493809842364-78817add7ffb?auto=format&fit=crop&w=800&q=80",
                null, null, null, null));

        demoRooms.add(new RoomListing(null, "P202", "Phòng trọ cao cấp có máy giặt riêng, view công viên",
                "View nhìn ra công viên cây xanh mát mẻ, phòng yên tĩnh tuyệt đối thích hợp làm việc online (WFH).",
                2, new BigDecimal("30.0"), new BigDecimal("4200000"), "DA_THUE",
                "Điều hòa, Wifi, Nóng lạnh, Máy giặt, Ban công, Khóa vân tay",
                "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80",
                "Lê Thị Thảo Nhi", "0987654321", LocalDate.of(2026, 3, 1), LocalDate.of(2027, 3, 1)));

        demoRooms.add(new RoomListing(null, "P203", "Phòng trọ duplex có gác gỗ thông cao cấp",
                "Trần cao 4.2m thoáng đãng, gác lát sàn gỗ thông tự nhiên thơm nhẹ, tủ quần áo âm tường sức chứa lớn.",
                2, new BigDecimal("26.0"), new BigDecimal("3500000"), "TRONG",
                "Điều hòa, Wifi, Nóng lạnh, Gác lửng, Tủ lạnh",
                "https://images.unsplash.com/photo-1554995207-c18c203602cb?auto=format&fit=crop&w=800&q=80",
                null, null, null, null));

        demoRooms.add(new RoomListing(null, "P301", "Phòng Penthouse mini tầng 3 sân thượng BBQ",
                "Phòng rộng có lối ra sân thượng ngắm trọn hoàng hôn thành phố. Được phép tổ chức tiệc BBQ cuối tuần.",
                3, new BigDecimal("42.0"), new BigDecimal("5500000"), "TRONG",
                "Điều hòa, Wifi, Nóng lạnh, Ban công, Máy giặt, Tủ lạnh, Sân thượng",
                "https://images.unsplash.com/photo-1502005229762-ee1b2b80a424?auto=format&fit=crop&w=800&q=80",
                null, null, null, null));

        demoRooms.add(new RoomListing(null, "P302", "Phòng trọ yên tĩnh dành cho người đi làm",
                "Khu vực dân trí cao, an ninh nghiêm ngặt với hệ thống camera AI nhận diện. Điện nước tính theo công tơ riêng chuẩn nhà nước.",
                3, new BigDecimal("22.0"), new BigDecimal("3000000"), "DA_THUE",
                "Điều hòa, Wifi, Nóng lạnh, Chỗ để xe",
                "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=800&q=80",
                "Phạm Đức Trọng", "0933221100", LocalDate.of(2025, 11, 10), LocalDate.of(2026, 11, 10)));

        demoRooms.add(new RoomListing(null, "P303", "Phòng đang nâng cấp thay mới sàn gỗ và máy lạnh",
                "Đang hoàn thiện nội thất mới tinh 100%, dự kiến bàn giao đón khách vào đầu tháng tới.",
                3, new BigDecimal("24.0"), new BigDecimal("3300000"), "BAO_TRI",
                "Điều hòa, Wifi, Nóng lạnh, Gác lửng",
                "https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?auto=format&fit=crop&w=800&q=80",
                null, null, null, null));

        demoRooms.add(new RoomListing(null, "P401", "Phòng trọ view Landmark cực đẹp, tầng cao thoáng khí",
                "Cửa sổ kính Low-E tràn viền cách âm cách nhiệt tốt. Buổi tối ngắm thành phố lên đèn cực chill.",
                4, new BigDecimal("32.0"), new BigDecimal("4500000"), "TRONG",
                "Điều hòa, Wifi, Nóng lạnh, Ban công, Khóa vân tay, Tủ lạnh",
                "https://images.unsplash.com/photo-1540518614846-7ede433c4ef4?auto=format&fit=crop&w=800&q=80",
                null, null, null, null));

        demoRooms.add(new RoomListing(null, "P402", "Căn hộ 2 giường đơn thích hợp bạn bè ở ghép",
                "Đã bố trí 2 giường đơn riêng tư kèm 2 bàn học/làm việc. Chi phí thuê siêu tiết kiệm khi chia đôi.",
                4, new BigDecimal("29.0"), new BigDecimal("3600000"), "TRONG",
                "Điều hòa, Wifi, Nóng lạnh, Bàn làm việc, Chỗ để xe",
                "https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=800&q=80",
                null, null, null, null));

        demoRooms.add(new RoomListing(null, "P403", "Phòng trọ ấm cúng tiện nghi, giá cực mềm",
                "Phòng sạch đẹp, dán tường cao cấp, nhà vệ sinh riêng khép kín có bồn tắm đứng hiện đại.",
                4, new BigDecimal("20.0"), new BigDecimal("2600000"), "DA_THUE",
                "Wifi, Nóng lạnh, Chỗ để xe",
                "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=800&q=80",
                "Vũ Minh Tuấn", "0977889900", LocalDate.of(2026, 2, 20), LocalDate.of(2027, 2, 20)));

        demoRooms.add(new RoomListing(null, "P501", "Phòng cao cấp áp mái phong cách Vintage cực chất",
                "Thiết kế tường gạch trần mộc mạc và cửa kính đón nắng bình minh. Không gian lý tưởng cho bạn trẻ thích sáng tạo.",
                5, new BigDecimal("36.0"), new BigDecimal("4600000"), "TRONG",
                "Điều hòa, Wifi, Nóng lạnh, Ban công, Tủ lạnh, Sofa",
                "https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=800&q=80",
                null, null, null, null));

        demoRooms.add(new RoomListing(null, "P502", "Phòng trọ đang sơn sửa chống thấm định kỳ",
                "Bảo trì định kỳ hệ thống đường ống nước và điện âm tường để đảm bảo an toàn tuyệt đối cho người thuê.",
                5, new BigDecimal("23.0"), new BigDecimal("2900000"), "BAO_TRI",
                "Wifi, Nóng lạnh, Chỗ để xe",
                "https://images.unsplash.com/photo-1507089947368-19c1da9775ae?auto=format&fit=crop&w=800&q=80",
                null, null, null, null));

        for (RoomListing r : demoRooms) {
            if (!roomListingRepository.existsByRoomCode(r.getRoomCode())) {
                roomListingRepository.save(r);
            }
        }
    }
}
