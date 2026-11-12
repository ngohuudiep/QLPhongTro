package com.qlphongtro.dto;

import com.qlphongtro.entity.RoomListing;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

public class PublicRoomDto {

    private final Integer listingId;
    private final String roomCode;
    private final String title;
    private final String description;
    private final Integer floorNo;
    private final BigDecimal area;
    private final BigDecimal price;
    private final String status;
    private final String amenities;
    private final String imageUrl;
    private final String imageGallery;

    private PublicRoomDto(RoomListing room) {
        this.listingId = room.getListingId();
        this.roomCode = room.getRoomCode();
        this.title = room.getTitle();
        this.description = room.getDescription();
        this.floorNo = room.getFloorNo();
        this.area = room.getArea();
        this.price = room.getPrice();
        this.status = room.getStatus();
        this.amenities = room.getAmenities();
        this.imageUrl = room.getImageUrl();
        this.imageGallery = room.getImageGallery();
    }

    public static PublicRoomDto from(RoomListing room) {
        return new PublicRoomDto(room);
    }

    public Integer getListingId() { return listingId; }
    public String getRoomCode() { return roomCode; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Integer getFloorNo() { return floorNo; }
    public BigDecimal getArea() { return area; }
    public BigDecimal getPrice() { return price; }
    public String getStatus() { return status; }
    public String getAmenities() { return amenities; }
    public String getImageUrl() { return imageUrl; }

    public List<String> getAmenityList() {
        if (amenities == null || amenities.trim().isEmpty()) {
            return List.of("Wifi", "Nóng lạnh");
        }
        return Arrays.stream(amenities.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    public List<String> getGalleryList() {
        if (imageGallery == null || imageGallery.trim().isEmpty()) {
            return List.of(imageUrl);
        }
        return Arrays.stream((imageUrl + "," + imageGallery).split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }

    public String getStatusLabel() {
        return switch (status) {
            case "TRONG" -> "Còn trống";
            case "DA_THUE" -> "Đã thuê";
            case "BAO_TRI" -> "Đang bảo trì";
            default -> status;
        };
    }

    public String getStatusBadgeClass() {
        return switch (status) {
            case "TRONG" -> "badge-status-empty";
            case "DA_THUE" -> "badge-status-rented";
            case "BAO_TRI" -> "badge-status-maintenance";
            default -> "badge-status-empty";
        };
    }

    public String getFormattedPrice() {
        if (price == null) {
            return "0 đ";
        }
        return new DecimalFormat("###,###,###").format(price) + " đ";
    }
}
