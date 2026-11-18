package com.qlphongtro.repository;

import com.qlphongtro.entity.StaffBuildingAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffBuildingAssignmentRepository extends JpaRepository<StaffBuildingAssignment, Integer> {
    List<StaffBuildingAssignment> findByStaff_Username(String username);
    boolean existsByStaff_UsernameAndBuilding_BuildingId(String username, Integer buildingId);
    boolean existsByStaff_UserIdAndBuilding_BuildingId(Integer userId, Integer buildingId);
}
