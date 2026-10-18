package com.qlphongtro.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "Room_Services")
public class RoomService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "price_id")
    private Integer priceId;

    // Quan hệ ManyToOne với Service (Bảng Services)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    // Quan hệ ManyToOne với Room (Bảng Rooms)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    public RoomService() {
    }

    public RoomService(Integer priceId, Service service, Room room, BigDecimal unitPrice, LocalDate effectiveFrom, LocalDate effectiveTo) {
        this.priceId = priceId;
        this.service = service;
        this.room = room;
        this.unitPrice = unitPrice;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
    }

    public Integer getPriceId() {
        return priceId;
    }

    public void setPriceId(Integer priceId) {
        this.priceId = priceId;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public static class RoomServiceBuilder {
        private Integer priceId;
        private Service service;
        private Room room;
        private BigDecimal unitPrice;
        private LocalDate effectiveFrom;
        private LocalDate effectiveTo;

        RoomServiceBuilder() {}

        public RoomServiceBuilder priceId(Integer priceId) {
            this.priceId = priceId;
            return this;
        }

        public RoomServiceBuilder service(Service service) {
            this.service = service;
            return this;
        }

        public RoomServiceBuilder room(Room room) {
            this.room = room;
            return this;
        }

        public RoomServiceBuilder unitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
            return this;
        }

        public RoomServiceBuilder effectiveFrom(LocalDate effectiveFrom) {
            this.effectiveFrom = effectiveFrom;
            return this;
        }

        public RoomServiceBuilder effectiveTo(LocalDate effectiveTo) {
            this.effectiveTo = effectiveTo;
            return this;
        }

        public RoomService build() {
            return new RoomService(priceId, service, room, unitPrice, effectiveFrom, effectiveTo);
        }
    }

    public static RoomServiceBuilder builder() {
        return new RoomServiceBuilder();
    }
}
