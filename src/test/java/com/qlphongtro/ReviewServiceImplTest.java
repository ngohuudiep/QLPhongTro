package com.qlphongtro;

import com.qlphongtro.entity.*;
import com.qlphongtro.repository.*;
import com.qlphongtro.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ContractRepository contractRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private RoomListingRepository roomListingRepository;
    @Mock private UserRepository userRepository;

    private ReviewServiceImpl reviewService;

    private User tenant;
    private User landlord;
    private Building building;
    private Room room;
    private Contract contract;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewServiceImpl(
                reviewRepository, contractRepository, roomRepository, roomListingRepository, userRepository);

        tenant = new User();
        tenant.setUserId(101);
        tenant.setUsername("tenant1");
        tenant.setFullName("Nguyen Van Tenant");

        landlord = new User();
        landlord.setUserId(202);
        landlord.setUsername("landlord1");
        landlord.setFullName("Tran Van Landlord");

        building = new Building();
        building.setBuildingId(1);
        building.setBuildingName("Toa nha A");
        building.setOwner(landlord);

        room = new Room();
        room.setRoomId(50);
        room.setRoomCode("P201");
        room.setBuilding(building);
        room.setStatus("DA_THUE");

        contract = new Contract();
        contract.setContractId(303);
        contract.setContractCode("HD-TEST-303");
        contract.setRoom(room);
        contract.setTenantUser(tenant);
        contract.setStatus("ACTIVE");
    }

    @Test
    @DisplayName("Khách thuê gửi đánh giá hợp lệ -> Lưu thành công và tính lại điểm")
    void createReviewSuccess() {
        when(userRepository.findByUsername("tenant1")).thenReturn(Optional.of(tenant));
        when(contractRepository.findById(303)).thenReturn(Optional.of(contract));
        when(reviewRepository.existsByContract_ContractId(303)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        when(reviewRepository.calculateAverageRatingForRoom(50)).thenReturn(4.8);
        when(reviewRepository.countVisibleReviewsForRoom(50)).thenReturn(1L);
        when(roomRepository.findById(50)).thenReturn(Optional.of(room));

        when(reviewRepository.calculateAverageRatingForLandlord(202)).thenReturn(4.8);
        when(reviewRepository.countVisibleReviewsForLandlord(202)).thenReturn(1L);
        when(userRepository.findById(202)).thenReturn(Optional.of(landlord));

        Review review = reviewService.createReview("tenant1", 303, 5, "Phòng rất sạch đẹp và thoáng mát!");

        assertNotNull(review);
        assertEquals(5, review.getRating());
        assertEquals("Phòng rất sạch đẹp và thoáng mát!", review.getComment());
        assertEquals(Review.STATUS_VISIBLE, review.getStatus());
        assertEquals(tenant, review.getTenantUser());
        assertEquals(landlord, review.getLandlordUser());

        verify(roomRepository).save(argThat(r -> r.getAverageRating().compareTo(new BigDecimal("4.80")) == 0 && r.getTotalReviews() == 1));
        verify(userRepository).save(argThat(u -> u.getAverageRating().compareTo(new BigDecimal("4.80")) == 0 && !u.isTopLandlord()));
    }

    @Test
    @DisplayName("Từ chối số sao không hợp lệ (ngoài khoảng 1-5)")
    void createReviewRejectsInvalidRating() {
        assertThrows(IllegalArgumentException.class, () ->
                reviewService.createReview("tenant1", 303, 0, "Quá tệ"));
        assertThrows(IllegalArgumentException.class, () ->
                reviewService.createReview("tenant1", 303, 6, "Quá tuyệt"));
    }

    @Test
    @DisplayName("Từ chối người thuê không thuộc hợp đồng")
    void createReviewRejectsUnrelatedTenant() {
        User otherTenant = new User();
        otherTenant.setUserId(999);
        otherTenant.setUsername("intruder");

        when(userRepository.findByUsername("intruder")).thenReturn(Optional.of(otherTenant));
        when(contractRepository.findById(303)).thenReturn(Optional.of(contract));

        assertThrows(AccessDeniedException.class, () ->
                reviewService.createReview("intruder", 303, 5, "Nhận xét trái phép"));
    }

    @Test
    @DisplayName("Từ chối hợp đồng chưa có hiệu lực (ví dụ DRAFT)")
    void createReviewRejectsDraftContract() {
        contract.setStatus("DRAFT");
        when(userRepository.findByUsername("tenant1")).thenReturn(Optional.of(tenant));
        when(contractRepository.findById(303)).thenReturn(Optional.of(contract));

        assertThrows(IllegalStateException.class, () ->
                reviewService.createReview("tenant1", 303, 5, "Đánh giá sớm"));
    }

    @Test
    @DisplayName("Từ chối đánh giá lặp lại cho cùng 1 hợp đồng")
    void createReviewRejectsDuplicateReview() {
        when(userRepository.findByUsername("tenant1")).thenReturn(Optional.of(tenant));
        when(contractRepository.findById(303)).thenReturn(Optional.of(contract));
        when(reviewRepository.existsByContract_ContractId(303)).thenReturn(true);

        assertThrows(IllegalStateException.class, () ->
                reviewService.createReview("tenant1", 303, 4, "Đánh giá lần 2"));
    }

    @Test
    @DisplayName("Chủ trọ phản hồi đánh giá thành công")
    void replyReviewSuccess() {
        Review review = new Review(contract, room, tenant, landlord, 5, "Tốt");
        review.setReviewId(1);

        when(userRepository.findByUsername("landlord1")).thenReturn(Optional.of(landlord));
        when(reviewRepository.findById(1)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        Review replied = reviewService.replyReview("landlord1", 1, "Cảm ơn bạn đã lựa chọn khu trọ!");

        assertEquals("Cảm ơn bạn đã lựa chọn khu trọ!", replied.getReplyComment());
        assertNotNull(replied.getRepliedAt());
    }

    @Test
    @DisplayName("Admin ẩn đánh giá vi phạm và tính lại điểm tự động")
    void toggleReviewVisibilityRecalculates() {
        Review review = new Review(contract, room, tenant, landlord, 1, "Nói xấu xúc phạm");
        review.setReviewId(2);

        when(reviewRepository.findById(2)).thenReturn(Optional.of(review));
        when(reviewRepository.calculateAverageRatingForRoom(50)).thenReturn(null);
        when(reviewRepository.countVisibleReviewsForRoom(50)).thenReturn(0L);
        when(roomRepository.findById(50)).thenReturn(Optional.of(room));

        when(reviewRepository.calculateAverageRatingForLandlord(202)).thenReturn(null);
        when(reviewRepository.countVisibleReviewsForLandlord(202)).thenReturn(0L);
        when(userRepository.findById(202)).thenReturn(Optional.of(landlord));

        reviewService.toggleReviewVisibility(2, false);

        assertEquals(Review.STATUS_HIDDEN, review.getStatus());
        verify(roomRepository).save(argThat(r -> r.getAverageRating().compareTo(BigDecimal.ZERO) == 0 && r.getTotalReviews() == 0));
    }

    @Test
    @DisplayName("Cơ chế tự động cấp huy hiệu Chủ trọ uy tín khi đạt >= 4.5 sao và >= 5 lượt đánh giá")
    void topLandlordBadgeAwardedWhenThresholdMet() {
        when(reviewRepository.calculateAverageRatingForLandlord(202)).thenReturn(4.65);
        when(reviewRepository.countVisibleReviewsForLandlord(202)).thenReturn(6L);
        when(userRepository.findById(202)).thenReturn(Optional.of(landlord));

        reviewService.recalculateRatings(null, 202);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedLandlord = captor.getValue();

        assertTrue(savedLandlord.isTopLandlord());
        assertEquals(new BigDecimal("4.65"), savedLandlord.getAverageRating());
        assertEquals(6, savedLandlord.getTotalReviews());
    }
}
