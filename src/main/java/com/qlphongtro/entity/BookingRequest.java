package com.qlphongtro.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity ánh xạ bảng BookingRequests trong SQL Server.
 * Lưu yêu cầu đặt/giữ phòng từ khách hàng (role USER).
 *
 * Luồng nghiệp vụ:
 *   1. Khách click "Đăng ký thuê" trên trang chủ → tạo record PENDING
 *   2. Staff/Admin vào /staff/requests xem danh sách PENDING
 *   3. Staff bấm "Duyệt" → status = APPROVED, phòng tự đổi sang DA_THUE
 *   4. Staff bấm "Từ chối" → status = REJECTED, phòng vẫn TRONG
 */
@Entity
@Table(name = "BookingRequests")
public class BookingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Integer requestId;

    /**
     * Người dùng gửi yêu cầu - quan hệ N-1 với Users.
     * Lấy từ SecurityContextHolder khi user đăng nhập click "Đăng ký thuê".
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Phòng muốn đặt - quan hệ N-1 với RoomListings.
     * Chỉ phòng có status TRONG mới được đặt.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomListing room;

    /** Thời điểm gửi yêu cầu - tự động set khi tạo mới */
    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;

    /**
     * Trạng thái xử lý yêu cầu:
     *   PENDING  - Chờ Staff/Admin xem xét
     *   APPROVED - Đã duyệt, phòng đổi sang DA_THUE tự động
     *   REJECTED - Đã từ chối, phòng vẫn TRONG
     */
    @Column(name = "status", nullable = false, length = 15)
    private String status = "PENDING";

    /** Ghi chú từ Staff khi duyệt hoặc từ chối */
    @Column(name = "note", length = 500)
    private String note;

    /** Staff/Admin xử lý yêu cầu này - nullable (chưa xử lý thì null) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_user_id")
    private User processedBy;

    /** Thời điểm Staff xử lý yêu cầu (duyệt hoặc từ chối) */
    @Column(name = "processed_date")
    private LocalDateTime processedDate;

    /** Tự động gán thời gian hiện tại khi tạo mới */
    @PrePersist
    public void prePersist() {
        if (this.requestDate == null) {
            this.requestDate = LocalDateTime.now();
        }
    }

    public BookingRequest() {}

    public BookingRequest(User user, RoomListing room) {
        this.user        = user;
        this.room        = room;
        this.status      = "PENDING";
        this.requestDate = LocalDateTime.now();
    }

    // ==================== Getters & Setters ====================

    public Integer getRequestId()                  { return requestId; }
    public void setRequestId(Integer requestId)    { this.requestId = requestId; }

    public User getUser()                          { return user; }
    public void setUser(User user)                 { this.user = user; }

    public RoomListing getRoom()                   { return room; }
    public void setRoom(RoomListing room)          { this.room = room; }

    public LocalDateTime getRequestDate()               { return requestDate; }
    public void setRequestDate(LocalDateTime requestDate) { this.requestDate = requestDate; }

    public String getStatus()                      { return status; }
    public void setStatus(String status)           { this.status = status; }

    public String getNote()                        { return note; }
    public void setNote(String note)               { this.note = note; }

    public User getProcessedBy()                   { return processedBy; }
    public void setProcessedBy(User processedBy)   { this.processedBy = processedBy; }

    public LocalDateTime getProcessedDate()             { return processedDate; }
    public void setProcessedDate(LocalDateTime d)       { this.processedDate = d; }

    /** Nhãn hiển thị trạng thái tiếng Việt - dùng trong Thymeleaf */
    public String getStatusLabel() {
        if (status == null) return "Không xác định";
        return switch (status) {
            case "PENDING"  -> "Chờ duyệt";
            case "APPROVED" -> "Đã duyệt";
            case "REJECTED" -> "Đã từ chối";
            default          -> status;
        };
    }
}
