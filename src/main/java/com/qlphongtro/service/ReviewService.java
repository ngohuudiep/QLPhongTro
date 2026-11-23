package com.qlphongtro.service;

import com.qlphongtro.entity.Contract;
import com.qlphongtro.entity.Review;
import com.qlphongtro.entity.RoomListing;
import com.qlphongtro.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ReviewService {

    Review createReview(String tenantUsername, Integer contractId, Integer rating, String comment);

    Review replyReview(String landlordUsername, Integer reviewId, String replyComment);

    void toggleReviewVisibility(Integer reviewId, boolean visible);

    void deleteReview(Integer reviewId);

    void recalculateRatings(Integer roomId, Integer landlordUserId);

    List<Review> getVisibleReviewsForRoom(Integer roomId);

    Page<Review> getVisibleReviewsForRoom(Integer roomId, Pageable pageable);

    Page<Review> getLandlordReviews(String landlordUsername, Pageable pageable);

    Page<Review> getAllReviewsForAdmin(String status, Pageable pageable);

    List<Contract> getReviewableContractsForTenant(String tenantUsername);

    Optional<Review> getReviewByContract(Integer contractId);

    List<User> getTopLandlords();

    List<RoomListing> getTopRatedRoomListings(int limit);
}
