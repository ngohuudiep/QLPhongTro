package com.qlphongtro.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "StaffBuildingAssignments", uniqueConstraints =
        @UniqueConstraint(name = "uk_staff_building_assignment", columnNames = {"staff_user_id", "building_id"}))
public class StaffBuildingAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Integer assignmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_user_id", nullable = false)
    private User staff;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    public Integer getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Integer assignmentId) { this.assignmentId = assignmentId; }
    public User getStaff() { return staff; }
    public void setStaff(User staff) { this.staff = staff; }
    public Building getBuilding() { return building; }
    public void setBuilding(Building building) { this.building = building; }
}
