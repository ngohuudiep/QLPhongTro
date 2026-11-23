package com.qlphongtro.service.impl;

import com.qlphongtro.entity.Contract;
import com.qlphongtro.entity.Review;
import com.qlphongtro.entity.Room;
import com.qlphongtro.entity.RoomListing;
import com.qlphongtro.entity.User;
import com.qlphongtro.repository.ContractRepository;
import com.qlphongtro.repository.ReviewRepository;
import com.qlphongtro.repository.RoomListingRepository;
import com.qlphongtro.repository.RoomRepository;
import com.qlphongtro.repository.UserRepository;
import com.qlphongtro.service.ReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private static final List<String> REVIEWABLE_STATUSES = List.of(
            "ACTIVE", "TERMINATED", "TENANT_ACCEPTED", "TERMINATION_REQUESTED", "HIỆU LỰC", "ĐÃ THANH LÝ"
    );

    private final ReviewRepository reviewRepository;
    private final ContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final RoomListingRepository roomListingRepository;
    private final UserRepository userRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             ContractRepository contractRepository,
                             RoomRepository roomRepository,
                             RoomListingRepository roomListingRepository,
                             UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.contractRepository = contractRepository;
        this.roomRepository = roomRepository;
        this.roomListingRepository = roomListingRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Review createReview(String tenantUsername, Integer contractId, Integer rating, String comment) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Điểm đánh giá phải từ 1 đến 5 sao.");
        }
        if (comment == null || comment.trim().isBlank()) {
            throw new IllegalArgumentException("Nội dung nhận xét không được để trống.");
        }
        if (comment.trim().length() > 2000) {
            throw new IllegalArgumentException("Nội dung nhận xét không được vượt quá 2000 ký tự.");
        }

        User tenant = userRepository.findByUsername(tenantUsername)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng: " + tenantUsername));

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hợp đồng #" + contractId));

        if (contract.getTenantUser() == null || !contract.getTenantUser().getUserId().equals(tenant.getUserId())) {
            throw new AccessDeniedException("Bạn không có quyền đánh giá hợp đồng này.");
        }

        boolean validStatus = contract.getStatus() != null &&
                REVIEWABLE_STATUSES.stream().anyMatch(s -> s.equalsIgnoreCase(contract.getStatus()));
        if (!validStatus) {
            throw new IllegalStateException("Chỉ hợp đồng đang có hiệu lực hoặc đã hoàn tất thuê mới có thể đánh giá.");
        }

        if (reviewRepository.existsByContract_ContractId(contractId)) {
            throw new IllegalStateException("Hợp đồng này đã được đánh giá trước đó. Mỗi hợp đồng chỉ được đánh giá 1 lần.");
        }

        Room room = contract.getRoom();
        if (room == null || room.getBuilding() == null || room.getBuilding().getOwner() == null) {
            throw new IllegalStateException("Không xác định được phòng hoặc chủ trọ liên quan đến hợp đồng.");
        }
        User landlord = room.getBuilding().getOwner();

        Review review = new Review(contract, room, tenant, landlord, rating, comment.trim());
        Review saved = reviewRepository.save(review);

        recalculateRatings(room.getRoomId(), landlord.getUserId());

        return saved;
    }

    @Override
    public Review replyReview(String landlordUsername, Integer reviewId, String replyComment) {
        if (replyComment == null || replyComment.trim().isBlank()) {
            throw new IllegalArgumentException("Nội dung phản hồi không được để trống.");
        }
        if (replyComment.trim().length() > 2000) {
            throw new IllegalArgumentException("Nội dung phản hồi không được vượt quá 2000 ký tự.");
        }

        User landlord = userRepository.findByUsername(landlordUsername)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chủ trọ: " + landlordUsername));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá #" + reviewId));

        if (!review.getLandlordUser().getUserId().equals(landlord.getUserId())) {
            throw new AccessDeniedException("Bạn không có quyền phản hồi đánh giá của phòng này.");
        }

        review.setReplyComment(replyComment.trim());
        review.setRepliedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());

        return reviewRepository.save(review);
    }

    @Override
    public void toggleReviewVisibility(Integer reviewId, boolean visible) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá #" + reviewId));

        review.setStatus(visible ? Review.STATUS_VISIBLE : Review.STATUS_HIDDEN);
        review.setUpdatedAt(LocalDateTime.now());
        reviewRepository.save(review);

        recalculateRatings(review.getRoom().getRoomId(), review.getLandlordUser().getUserId());
    }

    @Override
    public void deleteReview(Integer reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá #" + reviewId));

        Integer roomId = review.getRoom().getRoomId();
        Integer landlordUserId = review.getLandlordUser().getUserId();

        reviewRepository.delete(review);

        recalculateRatings(roomId, landlordUserId);
    }

    @Override
    public void recalculateRatings(Integer roomId, Integer landlordUserId) {
        if (roomId != null) {
            Double roomAvg = reviewRepository.calculateAverageRatingForRoom(roomId);
            Long roomTotal = reviewRepository.countVisibleReviewsForRoom(roomId);
            BigDecimal avgRoom = roomAvg != null ? BigDecimal.valueOf(roomAvg).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            int totalRoom = roomTotal != null ? roomTotal.intValue() : 0;

            roomRepository.findById(roomId).ifPresent(room -> {
                room.setAverageRating(avgRoom);
                room.setTotalReviews(totalRoom);
                roomRepository.save(room);
            });

            roomListingRepository.findByManagedRoom_RoomId(roomId).ifPresent(listing -> {
                listing.setAverageRating(avgRoom);
                listing.setTotalReviews(totalRoom);
                roomListingRepository.save(listing);
            });
        }

        if (landlordUserId != null) {
            Double landlordAvg = reviewRepository.calculateAverageRatingForLandlord(landlordUserId);
            Long landlordTotal = reviewRepository.countVisibleReviewsForLandlord(landlordUserId);
            BigDecimal avgLandlord = landlordAvg != null ? BigDecimal.valueOf(landlordAvg).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            int totalLandlord = landlordTotal != null ? landlordTotal.intValue() : 0;

            boolean isTop = avgLandlord.compareTo(new BigDecimal("4.50")) >= 0 && totalLandlord >= 5;

            userRepository.findById(landlordUserId).ifPresent(landlord -> {
                landlord.setAverageRating(avgLandlord);
                landlord.setTotalReviews(totalLandlord);
                landlord.setTopLandlord(isTop);
                userRepository.save(landlord);
            });
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getVisibleReviewsForRoom(Integer roomId) {
        return reviewRepository.findByRoom_RoomIdAndStatusOrderByCreatedAtDesc(roomId, Review.STATUS_VISIBLE);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Review> getVisibleReviewsForRoom(Integer roomId, Pageable pageable) {
        return reviewRepository.findByRoom_RoomIdAndStatusOrderByCreatedAtDesc(roomId, Review.STATUS_VISIBLE, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Review> getLandlordReviews(String landlordUsername, Pageable pageable) {
        User landlord = userRepository.findByUsername(landlordUsername)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chủ trọ: " + landlordUsername));
        return reviewRepository.findByLandlordUser_UserIdOrderByCreatedAtDesc(landlord.getUserId(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Review> getAllReviewsForAdmin(String status, Pageable pageable) {
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            return reviewRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase(), pageable);
        }
        return reviewRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Contract> getReviewableContractsForTenant(String tenantUsername) {
        User tenant = userRepository.findByUsername(tenantUsername)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng: " + tenantUsername));

        List<Contract> contracts = new java.util.ArrayList<>(contractRepository.findAllForTenant(tenantUsername));
        List<Contract> directContracts = contractRepository.findByTenantUser_UserId(tenant.getUserId());
        for (Contract dc : directContracts) {
            if (contracts.stream().noneMatch(c -> c.getContractId().equals(dc.getContractId()))) {
                contracts.add(dc);
            }
        }
        return contracts.stream()
                .filter(c -> c.getStatus() != null && REVIEWABLE_STATUSES.stream().anyMatch(s -> s.equalsIgnoreCase(c.getStatus())))
                .filter(c -> !reviewRepository.existsByContract_ContractId(c.getContractId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Review> getReviewByContract(Integer contractId) {
        return reviewRepository.findByContract_ContractId(contractId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getTopLandlords() {
        return userRepository.findTopLandlords();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomListing> getTopRatedRoomListings(int limit) {
        Sort sort = Sort.by(Sort.Direction.DESC, "averageRating").and(Sort.by(Sort.Direction.DESC, "totalReviews"));
        Pageable pageable = PageRequest.of(0, limit, sort);
        return roomListingRepository.findAll(pageable).getContent();
    }
}
