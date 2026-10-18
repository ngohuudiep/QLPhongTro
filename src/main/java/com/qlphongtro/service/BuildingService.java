package com.qlphongtro.service;

import com.qlphongtro.entity.Building;
import java.util.List;

public interface BuildingService {

    List<Building> getAllBuildings();

    Building getBuildingById(Integer id);

    Building saveBuilding(Building building);

    void deleteBuilding(Integer id);

    boolean existsByBuildingCode(String buildingCode);
}
