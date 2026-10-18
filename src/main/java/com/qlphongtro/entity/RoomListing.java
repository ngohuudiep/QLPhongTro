package com.qlphongtro.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity ánh xạ bảng RoomListings trong SQL Server.
 * Dùng để quản lý thông tin bài đăng phòng trọ cho thuê.
 * Tách biệt với entity Room cũ (quản lý nội bộ tòa nhà).
 */
@Entity
@Table(name = "RoomListings")
public class RoomListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "listing_id")
    private Integer listingId;

    /** Mã phòng - duy nhất trong hệ thống */
    @NotBlank(message = "Mã phòng không được để trống")
    @Size(max = 20, message = "Mã phòng tối đa 20 ký tự")
    @Column(name = "room_code", nullable = false, unique = true, length = 20)
    private String roomCode;

    /** Tiêu đề bài đăng phòng trọ */
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /** Mô tả chi tiết về phòng trọ */
    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    /** Diện tích phòng (m²) */
    @Column(name = "area", precision = 8, scale = 2)
    private BigDecimal area;

    /** Giá thuê hàng tháng (VNĐ) */
    @Column(name = "price", nullable = false, precision = 14, scale = 2)
    private BigDecimal price;

    /**
     * Trạng thái phòng:
     *   TRONG  - Phòng đang trống, có thể cho thuê
     *   DA_THUE - Phòng đã có người thuê
     *   BAO_TRI - Phòng đang bảo trì, không cho thuê
     */
    @Column(name = "status", nullable = false, length = 15)
    private String status = "TRONG";

    /** URL hình ảnh minh họa phòng trọ */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** Người tạo bài đăng - quan hệ N-1 với bảng Users */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    public RoomListing() {}

    public RoomListing(Integer listingId, String roomCode, String title, String description,
                       BigDecimal area, BigDecimal price, String status, String imageUrl, User createdBy) {
        this.listingId = listingId;
        this.roomCode = roomCode;
        this.title = title;
        this.description = description;
        this.area = area;
        this.price = price;
        this.status = status != null ? status : "TRONG";
        this.imageUrl = imageUrl;
        this.createdBy = createdBy;
    }

    // ==================== Getters & Setters ====================

    public Integer getListingId() { return listingId; }
    public void setListingId(Integer listingId) { this.listingId = listingId; }

    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    /**
     * Trả về nhãn hiển thị trạng thái phòng tiếng Việt.
     * Dùng trong Thymeleaf template: ${room.statusLabel}
     */
    public String getStatusLabel() {
        if (status == null) return "Không xác định";
        return switch (status) {
            case "TRONG"   -> "Còn trống";
            case "DA_THUE" -> "Đã thuê";
            case "BAO_TRI" -> "Bảo trì";
            default         -> status;
        };
    }
}
