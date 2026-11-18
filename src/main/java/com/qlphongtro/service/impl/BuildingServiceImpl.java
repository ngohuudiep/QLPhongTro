package com.qlphongtro.service.impl;

import com.qlphongtro.entity.Building;
import com.qlphongtro.repository.BuildingRepository;
import com.qlphongtro.security.BuildingAccess;
import com.qlphongtro.service.BuildingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BuildingServiceImpl implements BuildingService {

    private final BuildingRepository buildingRepository;
    private final BuildingAccess buildingAccess;

    @Autowired
    public BuildingServiceImpl(BuildingRepository buildingRepository, BuildingAccess buildingAccess) {
        this.buildingRepository = buildingRepository;
        this.buildingAccess = buildingAccess;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Transactional(readOnly = true)
    public List<Building> getAllBuildings() {
        if (buildingAccess.isStaff()) {
            List<Integer> buildingIds = buildingAccess.assignedBuildingIds();
            return buildingIds.isEmpty() ? List.of() : buildingRepository.findAllById(buildingIds);
        }
        return buildingRepository.findAll();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Transactional(readOnly = true)
    public Building getBuildingById(Integer id) {
        if (buildingAccess.isStaff() && !buildingAccess.canAccessBuilding(id)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        }
        return buildingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tòa nhà có ID: " + id));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Building saveBuilding(Building building) {
        return buildingRepository.save(building);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteBuilding(Integer id) {
        if (!buildingRepository.existsById(id)) {
            throw new RuntimeException("Không thể xóa, không tồn tại tòa nhà ID: " + id);
        }
        buildingRepository.deleteById(id);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Transactional(readOnly = true)
    public boolean existsByBuildingCode(String buildingCode) {
        return buildingRepository.existsByBuildingCode(buildingCode);
    }
}
