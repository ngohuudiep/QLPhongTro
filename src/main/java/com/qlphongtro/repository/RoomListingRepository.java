package com.qlphongtro.repository;

import com.qlphongtro.entity.RoomListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository thao tác dữ liệu bảng RoomListings.
 * Hỗ trợ tra cứu phòng cho khách xem và quản lý cho ADMIN/STAFF.
 */
@Repository
public interface RoomListingRepository extends JpaRepository<RoomListing, Integer> {

    /** Tìm phòng theo mã phòng */
    Optional<RoomListing> findByRoomCode(String roomCode);

    /**
     * Lấy tất cả phòng trống - dùng ở trang danh sách công khai cho khách xem.
     * Không cần đăng nhập để xem.
     */
    List<RoomListing> findByStatusOrderByPriceAsc(String status);

    /** Lấy tất cả phòng theo trạng thái (dùng cho ADMIN/STAFF) */
    List<RoomListing> findByStatus(String status);

    /**
     * Tìm kiếm phòng theo tiêu đề hoặc mô tả - hỗ trợ tìm kiếm cho khách.
     * Không phân biệt chữ hoa thường.
     */
    @Query("SELECT r FROM RoomListing r WHERE LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<RoomListing> searchByKeyword(@Param("keyword") String keyword);

    /**
     * Tìm phòng trống trong khoảng giá - dùng để filter khi khách tìm phòng.
     */
    @Query("SELECT r FROM RoomListing r WHERE r.status = 'TRONG' " +
           "AND r.price BETWEEN :minPrice AND :maxPrice ORDER BY r.price ASC")
    List<RoomListing> findAvailableByPriceRange(@Param("minPrice") BigDecimal minPrice,
                                                @Param("maxPrice") BigDecimal maxPrice);

    /** Kiểm tra mã phòng đã tồn tại chưa - dùng khi ADMIN tạo phòng mới */
    boolean existsByRoomCode(String roomCode);

    /** Đếm số phòng theo trạng thái - hiển thị thống kê trên dashboard */
    long countByStatus(String status);
}
