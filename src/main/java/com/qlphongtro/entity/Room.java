package com.qlphongtro.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Integer roomId;

    @Column(name = "room_code", nullable = false, length = 20)
    private String roomCode;

    // Quan hệ với Tòa nhà
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(name = "floor_no")
    private Short floorNo;

    @Column(name = "max_occupants")
    private Short maxOccupants;

    @Column(name = "price_override", precision = 14, scale = 2)
    private BigDecimal priceOverride;

    @Column(name = "status", length = 15)
    private String status;

    // Quan hệ 1-Nhiều với Room_Services (Bảng trung gian dịch vụ theo phòng)
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomService> roomServices = new ArrayList<>();

    // Quan hệ 1-Nhiều với MeterReadings (Chỉ số điện nước theo phòng)
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MeterReading> meterReadings = new ArrayList<>();

    public Room() {
    }

    public Room(Integer roomId, String roomCode, Building building, Short floorNo, Short maxOccupants, BigDecimal priceOverride, String status, List<RoomService> roomServices, List<MeterReading> meterReadings) {
        this.roomId = roomId;
        this.roomCode = roomCode;
        this.building = building;
        this.floorNo = floorNo;
        this.maxOccupants = maxOccupants;
        this.priceOverride = priceOverride;
        this.status = status;
        this.roomServices = roomServices != null ? roomServices : new ArrayList<>();
        this.meterReadings = meterReadings != null ? meterReadings : new ArrayList<>();
    }

    public Integer getRoomId() {
        return roomId;
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public Building getBuilding() {
        return building;
    }

    public void setBuilding(Building building) {
        this.building = building;
    }

    public Short getFloorNo() {
        return floorNo;
    }

    public void setFloorNo(Short floorNo) {
        this.floorNo = floorNo;
    }

    public Short getMaxOccupants() {
        return maxOccupants;
    }

    public void setMaxOccupants(Short maxOccupants) {
        this.maxOccupants = maxOccupants;
    }

    public BigDecimal getPriceOverride() {
        return priceOverride;
    }

    public void setPriceOverride(BigDecimal priceOverride) {
        this.priceOverride = priceOverride;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<RoomService> getRoomServices() {
        return roomServices;
    }

    public void setRoomServices(List<RoomService> roomServices) {
        this.roomServices = roomServices;
    }

    public List<MeterReading> getMeterReadings() {
        return meterReadings;
    }

    public void setMeterReadings(List<MeterReading> meterReadings) {
        this.meterReadings = meterReadings;
    }

    public static class RoomBuilder {
        private Integer roomId;
        private String roomCode;
        private Building building;
        private Short floorNo;
        private Short maxOccupants;
        private BigDecimal priceOverride;
        private String status;
        private List<RoomService> roomServices = new ArrayList<>();
        private List<MeterReading> meterReadings = new ArrayList<>();

        RoomBuilder() {}

        public RoomBuilder roomId(Integer roomId) {
            this.roomId = roomId;
            return this;
        }

        public RoomBuilder roomCode(String roomCode) {
            this.roomCode = roomCode;
            return this;
        }

        public RoomBuilder building(Building building) {
            this.building = building;
            return this;
        }

        public RoomBuilder floorNo(Short floorNo) {
            this.floorNo = floorNo;
            return this;
        }

        public RoomBuilder maxOccupants(Short maxOccupants) {
            this.maxOccupants = maxOccupants;
            return this;
        }

        public RoomBuilder priceOverride(BigDecimal priceOverride) {
            this.priceOverride = priceOverride;
            return this;
        }

        public RoomBuilder status(String status) {
            this.status = status;
            return this;
        }

        public RoomBuilder roomServices(List<RoomService> roomServices) {
            this.roomServices = roomServices;
            return this;
        }

        public RoomBuilder meterReadings(List<MeterReading> meterReadings) {
            this.meterReadings = meterReadings;
            return this;
        }

        public Room build() {
            return new Room(roomId, roomCode, building, floorNo, maxOccupants, priceOverride, status, roomServices, meterReadings);
        }
    }

    public static RoomBuilder builder() {
        return new RoomBuilder();
    }
}
