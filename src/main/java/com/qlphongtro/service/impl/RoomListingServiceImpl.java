package com.qlphongtro.service.impl;

import com.qlphongtro.entity.RoomListing;
import com.qlphongtro.repository.RoomListingRepository;
import com.qlphongtro.service.RoomListingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implementation của RoomListingService.
 * Xử lý logic nghiệp vụ cho bài đăng phòng trọ.
 */
@Service
@Transactional
public class RoomListingServiceImpl implements RoomListingService {

    private final RoomListingRepository roomListingRepository;

    @Autowired
    public RoomListingServiceImpl(RoomListingRepository roomListingRepository) {
        this.roomListingRepository = roomListingRepository;
    }

    /**
     * Lấy tất cả phòng (mọi trạng thái) - dùng cho trang quản lý ADMIN.
     * Admin cần thấy cả phòng đã thuê và bảo trì, không chỉ phòng trống.
     */
    @Override
    @Transactional(readOnly = true)
    public List<RoomListing> getAllRooms() {
        return roomListingRepository.findAll();
    }

    /**
     * Lấy tất cả phòng có trạng thái "TRONG" (còn trống), sắp xếp theo giá tăng dần.
     * Đây là dữ liệu hiển thị mặc định ở trang chủ khách hàng.
     */
    @Override
    @Transactional(readOnly = true)
    public List<RoomListing> getAvailableRooms() {
        return roomListingRepository.findByStatusOrderByPriceAsc("TRONG");
    }

    /**
     * Lọc phòng trống theo khoảng giá.
     * Dùng khi khách chọn bộ lọc giá trên trang chủ.
     */
    @Override
    @Transactional(readOnly = true)
    public List<RoomListing> getAvailableRoomsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return roomListingRepository.findAvailableByPriceRange(minPrice, maxPrice);
    }

    /**
     * Tìm kiếm phòng theo từ khóa.
     * Tìm trong tiêu đề và mô tả, không phân biệt hoa/thường.
     */
    @Override
    @Transactional(readOnly = true)
    public List<RoomListing> searchRooms(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAvailableRooms();
        }
        return roomListingRepository.searchByKeyword(keyword.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public RoomListing getById(Integer id) {
        return roomListingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public long countAvailableRooms() {
        return roomListingRepository.countByStatus("TRONG");
    }

    /**
     * Lưu hoặc cập nhật thông tin phòng (bao gồm giá tiền).
     * Logic:
     *   - Nếu listingId == null: Tạo mới, kiểm tra mã phòng chưa tồn tại.
     *   - Nếu listingId != null: Cập nhật, kiểm tra mã phòng không trùng với phòng khác.
     *   - Giá tiền được lưu trực tiếp vào cột price trong bảng RoomListings.
     *
     * @throws IllegalArgumentException nếu mã phòng đã tồn tại hoặc giá không hợp lệ
     */
    @Override
    public RoomListing save(RoomListing roomListing) {
        // Validate giá tiền
        if (roomListing.getPrice() == null || roomListing.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá phòng phải lớn hơn 0");
        }
        // Validate mã phòng trùng
        if (isRoomCodeTaken(roomListing.getRoomCode(), roomListing.getListingId())) {
            throw new IllegalArgumentException("Mã phòng '" + roomListing.getRoomCode() + "' đã tồn tại trong hệ thống");
        }
        // Trim các trường văn bản
        if (roomListing.getRoomCode() != null) roomListing.setRoomCode(roomListing.getRoomCode().trim().toUpperCase());
        if (roomListing.getTitle()    != null) roomListing.setTitle(roomListing.getTitle().trim());
        // Lưu vào SQL Server qua JPA
        return roomListingRepository.save(roomListing);
    }

    @Override
    public void delete(Integer id) {
        if (!roomListingRepository.existsById(id)) {
            throw new RuntimeException("Không thể xóa, phòng không tồn tại ID: " + id);
        }
        roomListingRepository.deleteById(id);
    }

    /**
     * Kiểm tra mã phòng đã tồn tại chưa.
     * - Khi tạo mới (excludeId = null): kiểm tra toàn bộ DB.
     * - Khi cập nhật (excludeId != null): bỏ qua chính record đang sửa.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isRoomCodeTaken(String roomCode, Integer excludeId) {
        return roomListingRepository.findByRoomCode(roomCode.trim().toUpperCase())
                .map(existing -> !existing.getListingId().equals(excludeId))
                .orElse(false);
    }
}
