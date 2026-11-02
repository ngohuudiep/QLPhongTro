package com.qlphongtro.repository;

import com.qlphongtro.entity.RoomListing;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository thao tác bảng RoomListings.
 * Hỗ trợ tra cứu đa tiêu chí: Trạng thái, Khoảng giá, Diện tích, Tầng, Từ khóa & Sắp xếp.
 */
@Repository
public interface RoomListingRepository extends JpaRepository<RoomListing, Integer> {

    Optional<RoomListing> findByRoomCode(String roomCode);

    List<RoomListing> findByStatusOrderByPriceAsc(String status);

    List<RoomListing> findByStatus(String status, Sort sort);

    List<RoomListing> findByStatus(String status);

    @Query("SELECT r FROM RoomListing r WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(r.roomCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(r.amenities) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:status IS NULL OR :status = '' OR :status = 'ALL' OR r.status = :status) AND " +
           "(:minPrice IS NULL OR r.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR r.price <= :maxPrice) AND " +
           "(:floorNo IS NULL OR r.floorNo = :floorNo)")
    List<RoomListing> searchAndFilter(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("floorNo") Integer floorNo,
            Sort sort);

    boolean existsByRoomCode(String roomCode);

    long countByStatus(String status);
}
