package com.qlphongtro.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity ánh xạ bảng BookingRequests trong SQL Server.
 * Lưu yêu cầu đặt/giữ phòng từ khách hàng (role TENANT / USER).
 */
@Entity
@Table(name = "BookingRequests")
public class BookingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Integer requestId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomListing room;

    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "full_name", length = 150, columnDefinition = "NVARCHAR(150)")
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "id_number", length = 20)
    private String idNumber;

    @Column(name = "expected_occupants")
    private Short expectedOccupants;

    @Column(name = "expected_move_in_date")
    private LocalDate expectedMoveInDate;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "note", columnDefinition = "NVARCHAR(500)")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_user_id")
    private User processedBy;

    @Column(name = "processed_date")
    private LocalDateTime processedDate;

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

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }

    public Short getExpectedOccupants() { return expectedOccupants; }
    public void setExpectedOccupants(Short expectedOccupants) { this.expectedOccupants = expectedOccupants; }

    public LocalDate getExpectedMoveInDate() { return expectedMoveInDate; }
    public void setExpectedMoveInDate(LocalDate expectedMoveInDate) { this.expectedMoveInDate = expectedMoveInDate; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

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
            case "PENDING_LANDLORD_REVIEW" -> "Đang chờ chủ trọ xác nhận";
            case "APPROVED_BY_LANDLORD" -> "Đã duyệt — chờ gửi hợp đồng";
            case "CONTRACT_SENT" -> "Đang chờ người thuê ký";
            case "COMPLETED" -> "Đã hoàn tất thuê";
            case "REJECTED_BY_LANDLORD" -> "Đã từ chối";
            case "CANCELLED" -> "Đã hủy";
            case "PENDING"  -> "Chờ duyệt";
            case "APPROVED" -> "Đã duyệt";
            case "REJECTED" -> "Đã từ chối";
            default          -> status;
        };
    }
}
