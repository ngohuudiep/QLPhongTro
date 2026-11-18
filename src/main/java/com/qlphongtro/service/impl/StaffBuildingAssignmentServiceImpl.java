package com.qlphongtro.service.impl;

import com.qlphongtro.entity.Building;
import com.qlphongtro.entity.StaffBuildingAssignment;
import com.qlphongtro.entity.User;
import com.qlphongtro.repository.BuildingRepository;
import com.qlphongtro.repository.StaffBuildingAssignmentRepository;
import com.qlphongtro.repository.UserRepository;
import com.qlphongtro.service.StaffBuildingAssignmentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
@PreAuthorize("hasRole('ADMIN')")
public class StaffBuildingAssignmentServiceImpl implements StaffBuildingAssignmentService {
    private final StaffBuildingAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;

    public StaffBuildingAssignmentServiceImpl(StaffBuildingAssignmentRepository assignmentRepository,
                                             UserRepository userRepository,
                                             BuildingRepository buildingRepository) {
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.buildingRepository = buildingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffBuildingAssignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getStaffUsers() {
        return userRepository.findByRole_RoleName("STAFF");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Building> getBuildings() {
        return buildingRepository.findAll();
    }

    @Override
    public StaffBuildingAssignment assign(Integer staffUserId, Integer buildingId) {
        User staff = userRepository.findById(staffUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (staff.getRole() == null || !"STAFF".equals(staff.getRole().getRoleName())) {
            throw new IllegalArgumentException("Only STAFF accounts can receive building assignments.");
        }
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (assignmentRepository.existsByStaff_UserIdAndBuilding_BuildingId(staffUserId, buildingId)) {
            throw new IllegalArgumentException("This staff member is already assigned to that building.");
        }
        StaffBuildingAssignment assignment = new StaffBuildingAssignment();
        assignment.setStaff(staff);
        assignment.setBuilding(building);
        return assignmentRepository.save(assignment);
    }

    @Override
    public void remove(Integer assignmentId) {
        if (!assignmentRepository.existsById(assignmentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        assignmentRepository.deleteById(assignmentId);
    }
}
