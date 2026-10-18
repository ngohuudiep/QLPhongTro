package com.qlphongtro.service.impl;

import com.qlphongtro.entity.Building;
import com.qlphongtro.repository.BuildingRepository;
import com.qlphongtro.service.BuildingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BuildingServiceImpl implements BuildingService {

    private final BuildingRepository buildingRepository;

    @Autowired
    public BuildingServiceImpl(BuildingRepository buildingRepository) {
        this.buildingRepository = buildingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Building> getAllBuildings() {
        return buildingRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Building getBuildingById(Integer id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tòa nhà có ID: " + id));
    }

    @Override
    public Building saveBuilding(Building building) {
        return buildingRepository.save(building);
    }

    @Override
    public void deleteBuilding(Integer id) {
        if (!buildingRepository.existsById(id)) {
            throw new RuntimeException("Không thể xóa, không tồn tại tòa nhà ID: " + id);
        }
        buildingRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByBuildingCode(String buildingCode) {
        return buildingRepository.existsByBuildingCode(buildingCode);
    }
}
