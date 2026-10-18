package com.qlphongtro.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "MeterReadings")
public class MeterReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reading_id")
    private Integer readingId;

    // Quan hệ ManyToOne với Room (Bảng Rooms)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "billing_month", nullable = false)
    private LocalDate billingMonth;

    @Column(name = "start_index", nullable = false, precision = 12, scale = 2)
    private BigDecimal startIndex;

    @Column(name = "end_index", nullable = false, precision = 12, scale = 2)
    private BigDecimal endIndex;

    @Column(name = "consumption", nullable = false, precision = 12, scale = 2)
    private BigDecimal consumption;

    @Column(name = "read_date", nullable = false)
    private LocalDate readDate;

    // Tự động tính số tiêu thụ (consumption = endIndex - startIndex) trước khi lưu
    @PrePersist
    @PreUpdate
    public void calculateConsumption() {
        if (startIndex != null && endIndex != null) {
            this.consumption = endIndex.subtract(startIndex);
        }
    }

    public MeterReading() {
    }

    public MeterReading(Integer readingId, Room room, LocalDate billingMonth, BigDecimal startIndex, BigDecimal endIndex, BigDecimal consumption, LocalDate readDate) {
        this.readingId = readingId;
        this.room = room;
        this.billingMonth = billingMonth;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.consumption = consumption;
        this.readDate = readDate;
    }

    public Integer getReadingId() {
        return readingId;
    }

    public void setReadingId(Integer readingId) {
        this.readingId = readingId;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public LocalDate getBillingMonth() {
        return billingMonth;
    }

    public void setBillingMonth(LocalDate billingMonth) {
        this.billingMonth = billingMonth;
    }

    public BigDecimal getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(BigDecimal startIndex) {
        this.startIndex = startIndex;
    }

    public BigDecimal getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(BigDecimal endIndex) {
        this.endIndex = endIndex;
    }

    public BigDecimal getConsumption() {
        return consumption;
    }

    public void setConsumption(BigDecimal consumption) {
        this.consumption = consumption;
    }

    public LocalDate getReadDate() {
        return readDate;
    }

    public void setReadDate(LocalDate readDate) {
        this.readDate = readDate;
    }

    public static class MeterReadingBuilder {
        private Integer readingId;
        private Room room;
        private LocalDate billingMonth;
        private BigDecimal startIndex;
        private BigDecimal endIndex;
        private BigDecimal consumption;
        private LocalDate readDate;

        MeterReadingBuilder() {}

        public MeterReadingBuilder readingId(Integer readingId) {
            this.readingId = readingId;
            return this;
        }

        public MeterReadingBuilder room(Room room) {
            this.room = room;
            return this;
        }

        public MeterReadingBuilder billingMonth(LocalDate billingMonth) {
            this.billingMonth = billingMonth;
            return this;
        }

        public MeterReadingBuilder startIndex(BigDecimal startIndex) {
            this.startIndex = startIndex;
            return this;
        }

        public MeterReadingBuilder endIndex(BigDecimal endIndex) {
            this.endIndex = endIndex;
            return this;
        }

        public MeterReadingBuilder consumption(BigDecimal consumption) {
            this.consumption = consumption;
            return this;
        }

        public MeterReadingBuilder readDate(LocalDate readDate) {
            this.readDate = readDate;
            return this;
        }

        public MeterReading build() {
            return new MeterReading(readingId, room, billingMonth, startIndex, endIndex, consumption, readDate);
        }
    }

    public static MeterReadingBuilder builder() {
        return new MeterReadingBuilder();
    }
}
