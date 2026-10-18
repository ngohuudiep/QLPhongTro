package com.qlphongtro.service;

import com.qlphongtro.entity.RoomListing;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service interface cho bài đăng phòng trọ (RoomListing).
 * Được dùng bởi HomeController để hiển thị danh sách phòng cho khách.
 */
public interface RoomListingService {

    /** Lấy TẤT CẢ phòng (mọi trạng thái) - hiển thị ở trang admin */
    List<RoomListing> getAllRooms();

    /** Lấy tất cả phòng đang TRỐNG - hiển thị ở trang chủ khách hàng */
    List<RoomListing> getAvailableRooms();

    /** Lấy phòng trống và lọc theo khoảng giá */
    List<RoomListing> getAvailableRoomsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);

    /** Tìm kiếm phòng theo từ khóa trong tiêu đề / mô tả */
    List<RoomListing> searchRooms(String keyword);

    /** Lấy phòng theo ID để xem chi tiết */
    RoomListing getById(Integer id);

    /** Đếm số phòng trống hiện tại - hiển thị trên banner */
    long countAvailableRooms();

    /** Lưu/cập nhật thông tin phòng (dùng cho ADMIN) */
    RoomListing save(RoomListing roomListing);

    /** Xóa phòng theo ID (dùng cho ADMIN) */
    void delete(Integer id);

    /** Kiểm tra mã phòng đã tồn tại chưa (trừ chính record hiện tại khi sửa) */
    boolean isRoomCodeTaken(String roomCode, Integer excludeId);
}
