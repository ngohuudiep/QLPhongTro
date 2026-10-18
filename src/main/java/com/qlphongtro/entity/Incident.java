package com.qlphongtro.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "incident_id")
    private Integer incidentId;

    // Quan hệ ManyToOne tới Rooms
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "priority", length = 10)
    private String priority; // High, Medium, Low

    @Column(name = "status", length = 15)
    private String status; // "Tiếp nhận", "Đang xử lý", "Hoàn thành", "Đã hủy"

    @Column(name = "repair_cost", precision = 14, scale = 2)
    private BigDecimal repairCost;

    @Column(name = "reported_at")
    private LocalDateTime reportedAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public Incident() {
    }

    public Incident(Integer incidentId, Room room, String title, String priority, String status, BigDecimal repairCost, LocalDateTime reportedAt, LocalDateTime resolvedAt) {
        this.incidentId = incidentId;
        this.room = room;
        this.title = title;
        this.priority = priority;
        this.status = status;
        this.repairCost = repairCost;
        this.reportedAt = reportedAt != null ? reportedAt : LocalDateTime.now();
        this.resolvedAt = resolvedAt;
    }

    public Integer getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(Integer incidentId) {
        this.incidentId = incidentId;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getRepairCost() {
        return repairCost;
    }

    public void setRepairCost(BigDecimal repairCost) {
        this.repairCost = repairCost;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public static class IncidentBuilder {
        private Integer incidentId;
        private Room room;
        private String title;
        private String priority;
        private String status;
        private BigDecimal repairCost;
        private LocalDateTime reportedAt = LocalDateTime.now();
        private LocalDateTime resolvedAt;

        IncidentBuilder() {}

        public IncidentBuilder incidentId(Integer incidentId) {
            this.incidentId = incidentId;
            return this;
        }

        public IncidentBuilder room(Room room) {
            this.room = room;
            return this;
        }

        public IncidentBuilder title(String title) {
            this.title = title;
            return this;
        }

        public IncidentBuilder priority(String priority) {
            this.priority = priority;
            return this;
        }

        public IncidentBuilder status(String status) {
            this.status = status;
            return this;
        }

        public IncidentBuilder repairCost(BigDecimal repairCost) {
            this.repairCost = repairCost;
            return this;
        }

        public IncidentBuilder reportedAt(LocalDateTime reportedAt) {
            this.reportedAt = reportedAt;
            return this;
        }

        public IncidentBuilder resolvedAt(LocalDateTime resolvedAt) {
            this.resolvedAt = resolvedAt;
            return this;
        }

        public Incident build() {
            return new Incident(incidentId, room, title, priority, status, repairCost, reportedAt, resolvedAt);
        }
    }

    public static IncidentBuilder builder() {
        return new IncidentBuilder();
    }
}
