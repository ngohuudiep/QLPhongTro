package com.qlphongtro.repository;

import com.qlphongtro.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    Optional<Review> findByContract_ContractId(Integer contractId);

    boolean existsByContract_ContractId(Integer contractId);

    List<Review> findByRoom_RoomIdAndStatusOrderByCreatedAtDesc(Integer roomId, String status);

    Page<Review> findByRoom_RoomIdAndStatusOrderByCreatedAtDesc(Integer roomId, String status, Pageable pageable);

    List<Review> findByLandlordUser_UserIdOrderByCreatedAtDesc(Integer landlordUserId);

    Page<Review> findByLandlordUser_UserIdOrderByCreatedAtDesc(Integer landlordUserId, Pageable pageable);

    Page<Review> findByLandlordUser_UserIdAndStatusOrderByCreatedAtDesc(Integer landlordUserId, String status, Pageable pageable);

    List<Review> findByTenantUser_UserIdOrderByCreatedAtDesc(Integer tenantUserId);

    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Review> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    @Query("SELECT AVG(CAST(r.rating AS double)) FROM Review r WHERE r.room.roomId = :roomId AND r.status = 'VISIBLE'")
    Double calculateAverageRatingForRoom(@Param("roomId") Integer roomId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.room.roomId = :roomId AND r.status = 'VISIBLE'")
    Long countVisibleReviewsForRoom(@Param("roomId") Integer roomId);

    @Query("SELECT AVG(CAST(r.rating AS double)) FROM Review r WHERE r.landlordUser.userId = :landlordUserId AND r.status = 'VISIBLE'")
    Double calculateAverageRatingForLandlord(@Param("landlordUserId") Integer landlordUserId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.landlordUser.userId = :landlordUserId AND r.status = 'VISIBLE'")
    Long countVisibleReviewsForLandlord(@Param("landlordUserId") Integer landlordUserId);
}
