package com.qlphongtro.repository;

import com.qlphongtro.entity.BookingRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository thao tác dữ liệu bảng BookingRequests.
 */
@Repository
public interface BookingRequestRepository extends JpaRepository<BookingRequest, Integer> {

    /**
     * Lấy tất cả yêu cầu, sắp xếp: PENDING lên trước, sau đó theo thời gian mới nhất.
     * Dùng ở trang staff/requests.html để Staff thấy việc cần làm trước.
     */
    @Query("SELECT b FROM BookingRequest b " +
           "ORDER BY CASE b.status WHEN 'PENDING' THEN 0 WHEN 'APPROVED' THEN 1 ELSE 2 END, " +
           "b.requestDate DESC")
    List<BookingRequest> findAllOrderByStatusThenDate();

    /** Chỉ lấy yêu cầu PENDING - dùng cho badge thông báo số lượng chờ xử lý */
    List<BookingRequest> findByStatusOrderByRequestDateAsc(String status);

    /** Lấy lịch sử yêu cầu của một User cụ thể */
    List<BookingRequest> findByUser_UserIdOrderByRequestDateDesc(Integer userId);

    /** Kiểm tra user đã gửi yêu cầu PENDING cho phòng này chưa */
    boolean existsByUser_UserIdAndRoom_ListingIdAndStatus(Integer userId, Integer listingId, String status);

    /** Đếm số yêu cầu đang PENDING - hiển thị badge trên navbar/dashboard */
    long countByStatus(String status);
}
