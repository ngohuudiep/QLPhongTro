package com.qlphongtro.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "Assets")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_asset_id")
    private Integer roomAssetId;

    // Quan hệ ManyToOne tới Rooms
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "asset_name", nullable = false, length = 150)
    private String assetName;

    @Column(name = "quantity")
    private Short quantity = 1;

    @Column(name = "condition_status", length = 50)
    private String conditionStatus; // Ví dụ: "Mới 100%", "Bình thường", "Cần bảo trì", "Hỏng"

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    public Asset() {
    }

    public Asset(Integer roomAssetId, Room room, String assetName, Short quantity, String conditionStatus, LocalDate purchaseDate) {
        this.roomAssetId = roomAssetId;
        this.room = room;
        this.assetName = assetName;
        this.quantity = quantity;
        this.conditionStatus = conditionStatus;
        this.purchaseDate = purchaseDate;
    }

    public Integer getRoomAssetId() {
        return roomAssetId;
    }

    public void setRoomAssetId(Integer roomAssetId) {
        this.roomAssetId = roomAssetId;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public Short getQuantity() {
        return quantity;
    }

    public void setQuantity(Short quantity) {
        this.quantity = quantity;
    }

    public String getConditionStatus() {
        return conditionStatus;
    }

    public void setConditionStatus(String conditionStatus) {
        this.conditionStatus = conditionStatus;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public static class AssetBuilder {
        private Integer roomAssetId;
        private Room room;
        private String assetName;
        private Short quantity = 1;
        private String conditionStatus;
        private LocalDate purchaseDate;

        AssetBuilder() {}

        public AssetBuilder roomAssetId(Integer roomAssetId) {
            this.roomAssetId = roomAssetId;
            return this;
        }

        public AssetBuilder room(Room room) {
            this.room = room;
            return this;
        }

        public AssetBuilder assetName(String assetName) {
            this.assetName = assetName;
            return this;
        }

        public AssetBuilder quantity(Short quantity) {
            this.quantity = quantity;
            return this;
        }

        public AssetBuilder conditionStatus(String conditionStatus) {
            this.conditionStatus = conditionStatus;
            return this;
        }

        public AssetBuilder purchaseDate(LocalDate purchaseDate) {
            this.purchaseDate = purchaseDate;
            return this;
        }

        public Asset build() {
            return new Asset(roomAssetId, room, assetName, quantity, conditionStatus, purchaseDate);
        }
    }

    public static AssetBuilder builder() {
        return new AssetBuilder();
    }
}
