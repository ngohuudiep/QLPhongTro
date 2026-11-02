package com.qlphongtro.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Entity ánh xạ bảng RoomListings trong SQL Server.
 * Quản lý thông tin bài đăng và chi tiết phòng trọ cho thuê.
 */
@Entity
@Table(name = "RoomListings")
public class RoomListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "listing_id")
    private Integer listingId;

    /** Mã phòng - duy nhất */
    @NotBlank(message = "Mã phòng không được để trống")
    @Size(max = 20, message = "Mã phòng tối đa 20 ký tự")
    @Column(name = "room_code", nullable = false, unique = true, length = 20)
    private String roomCode;

    /** Tiêu đề / Tên phòng (Hỗ trợ tiếng Việt Unicode) */
    @NotBlank(message = "Tiêu đề phòng không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    @Column(name = "title", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String title;

    /** Mô tả chi tiết phòng */
    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    /** Tầng (Floor) */
    @Column(name = "floor_no")
    private Integer floorNo = 1;

    /** Diện tích phòng (m²) */
    @NotNull(message = "Diện tích không được để trống")
    @DecimalMin(value = "5.0", message = "Diện tích tối thiểu 5m²")
    @Column(name = "area", precision = 8, scale = 2)
    private BigDecimal area;

    /** Giá thuê tháng (VNĐ) */
    @NotNull(message = "Giá thuê không được để trống")
    @DecimalMin(value = "100000", message = "Giá thuê tối thiểu 100,000 VNĐ")
    @Column(name = "price", nullable = false, precision = 14, scale = 2)
    private BigDecimal price;

    /**
     * Trạng thái:
     *   TRONG   - Còn trống
     *   DA_THUE - Đã cho thuê
     *   BAO_TRI - Đang sửa chữa / bảo trì
     */
    @Column(name = "status", nullable = false, length = 15)
    private String status = "TRONG";

    /** Danh sách tiện nghi (NVARCHAR hỗ trợ tiếng Việt có dấu) */
    @Column(name = "amenities", columnDefinition = "NVARCHAR(500)")
    private String amenities = "Điều hòa, Wifi, Nóng lạnh";

    /** URL ảnh đại diện chính */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** Danh sách URL ảnh bổ sung */
    @Column(name = "image_gallery", columnDefinition = "NVARCHAR(MAX)")
    private String imageGallery;

    /** Thông tin người đang thuê */
    @Column(name = "current_tenant", columnDefinition = "NVARCHAR(150)")
    private String currentTenant;

    @Column(name = "tenant_phone", length = 20)
    private String tenantPhone;

    @Column(name = "lease_start_date")
    private LocalDate leaseStartDate;

    @Column(name = "lease_end_date")
    private LocalDate leaseEndDate;

    /** Thời gian tạo bài đăng */
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Người tạo (Chủ trọ) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    public RoomListing() {}

    public RoomListing(Integer listingId, String roomCode, String title, String description, Integer floorNo,
                       BigDecimal area, BigDecimal price, String status, String amenities, String imageUrl,
                       String currentTenant, String tenantPhone, LocalDate leaseStartDate, LocalDate leaseEndDate) {
        this.listingId = listingId;
        this.roomCode = roomCode;
        this.title = title;
        this.description = description;
        this.floorNo = floorNo;
        this.area = area;
        this.price = price;
        this.status = status != null ? status : "TRONG";
        this.amenities = amenities;
        this.imageUrl = imageUrl;
        this.currentTenant = currentTenant;
        this.tenantPhone = tenantPhone;
        this.leaseStartDate = leaseStartDate;
        this.leaseEndDate = leaseEndDate;
        this.createdAt = LocalDateTime.now();
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

    public Integer getFloorNo() { return floorNo != null ? floorNo : 1; }
    public void setFloorNo(Integer floorNo) { this.floorNo = floorNo; }

    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getStatus() { return status != null ? status : "TRONG"; }
    public void setStatus(String status) { this.status = status; }

    public String getAmenities() { return amenities; }
    public void setAmenities(String amenities) { this.amenities = amenities; }

    public String getImageUrl() {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return "/images/room-placeholder.svg";
        }
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getImageGallery() { return imageGallery; }
    public void setImageGallery(String imageGallery) { this.imageGallery = imageGallery; }

    public String getCurrentTenant() { return currentTenant; }
    public void setCurrentTenant(String currentTenant) { this.currentTenant = currentTenant; }

    public String getTenantPhone() { return tenantPhone; }
    public void setTenantPhone(String tenantPhone) { this.tenantPhone = tenantPhone; }

    public LocalDate getLeaseStartDate() { return leaseStartDate; }
    public void setLeaseStartDate(LocalDate leaseStartDate) { this.leaseStartDate = leaseStartDate; }

    public LocalDate getLeaseEndDate() { return leaseEndDate; }
    public void setLeaseEndDate(LocalDate leaseEndDate) { this.leaseEndDate = leaseEndDate; }

    public LocalDateTime getCreatedAt() { return createdAt != null ? createdAt : LocalDateTime.now(); }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    // ==================== View Helper Methods ====================

    public String getStatusLabel() {
        if (status == null) return "Còn trống";
        return switch (status) {
            case "TRONG"   -> "Còn trống";
            case "DA_THUE" -> "Đã thuê";
            case "BAO_TRI" -> "Đang bảo trì";
            default         -> status;
        };
    }

    public String getStatusBadgeClass() {
        if (status == null) return "badge-status-empty";
        return switch (status) {
            case "TRONG"   -> "badge-status-empty";
            case "DA_THUE" -> "badge-status-rented";
            case "BAO_TRI" -> "badge-status-maintenance";
            default         -> "badge-status-empty";
        };
    }

    public List<String> getAmenityList() {
        if (amenities == null || amenities.trim().isEmpty()) {
            return List.of("Wifi", "Nóng lạnh");
        }
        return Arrays.stream(amenities.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public List<String> getGalleryList() {
        List<String> list = new ArrayList<>();
        list.add(getImageUrl());
        if (imageGallery != null && !imageGallery.trim().isEmpty()) {
            Arrays.stream(imageGallery.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !s.equalsIgnoreCase(getImageUrl()))
                    .forEach(list::add);
        }
        return list;
    }

    public String getFormattedPrice() {
        if (price == null) return "0 đ";
        DecimalFormat df = new DecimalFormat("###,###,###");
        return df.format(price) + " đ";
    }
}
