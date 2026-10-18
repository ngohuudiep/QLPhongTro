package com.qlphongtro.repository;

import com.qlphongtro.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BuildingRepository extends JpaRepository<Building, Integer> {

    // Tìm kiếm tòa nhà theo mã tòa nhà (building_code)
    Optional<Building> findByBuildingCode(String buildingCode);

    // Kiểm tra xem mã tòa nhà đã tồn tại chưa
    boolean existsByBuildingCode(String buildingCode);
}
