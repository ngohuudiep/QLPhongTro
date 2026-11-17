package com.qlphongtro.service;

import com.qlphongtro.entity.Building;
import com.qlphongtro.entity.StaffBuildingAssignment;
import com.qlphongtro.entity.User;

import java.util.List;

public interface StaffBuildingAssignmentService {
    List<StaffBuildingAssignment> getAllAssignments();
    List<User> getStaffUsers();
    List<Building> getBuildings();
    StaffBuildingAssignment assign(Integer staffUserId, Integer buildingId);
    void remove(Integer assignmentId);
}
