package com.qlphongtro.security;

import com.qlphongtro.entity.Building;
import com.qlphongtro.entity.Room;
import com.qlphongtro.repository.StaffBuildingAssignmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class BuildingAccess {
    private final StaffBuildingAssignmentRepository assignmentRepository;

    public BuildingAccess(StaffBuildingAssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    public boolean isStaff() {
        return CurrentUser.hasRole("STAFF") && !CurrentUser.hasRole("ADMIN") && !CurrentUser.hasRole("LANDLORD");
    }

    public List<Integer> assignedBuildingIds() {
        String username = CurrentUser.username();
        if (!isStaff() || username == null) {
            return List.of();
        }
        return assignmentRepository.findByStaff_Username(username).stream()
                .map(assignment -> assignment.getBuilding().getBuildingId())
                .distinct()
                .toList();
    }

    public boolean canAccessBuilding(Integer buildingId) {
        return !isStaff() || (buildingId != null && CurrentUser.username() != null
                && assignmentRepository.existsByStaff_UsernameAndBuilding_BuildingId(CurrentUser.username(), buildingId));
    }

    public void requireBuilding(Integer buildingId) {
        if (!canAccessBuilding(buildingId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    public void requireRoom(Room room) {
        requireBuilding(room == null || room.getBuilding() == null ? null : room.getBuilding().getBuildingId());
    }

    public boolean canAccessRoom(Room room) {
        return room != null && room.getBuilding() != null && canAccessBuilding(room.getBuilding().getBuildingId());
    }
}
