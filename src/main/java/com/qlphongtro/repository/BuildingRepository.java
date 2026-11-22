package com.qlphongtro.repository;

import com.qlphongtro.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface BuildingRepository extends JpaRepository<Building, Integer> {

    // Tìm kiếm tòa nhà theo mã tòa nhà (building_code)
    Optional<Building> findByBuildingCode(String buildingCode);

    // Kiểm tra xem mã tòa nhà đã tồn tại chưa
    boolean existsByBuildingCode(String buildingCode);

    List<Building> findByOwner_UsernameOrderByBuildingNameAsc(String username);

    Optional<Building> findByBuildingIdAndOwner_Username(Integer buildingId, String username);

    boolean existsByBuildingCodeAndBuildingIdNot(String buildingCode, Integer buildingId);

    boolean existsByOwner_UsernameAndBuildingCode(String username, String buildingCode);

    boolean existsByOwner_UserIdAndBuildingNameIgnoreCase(Integer ownerId, String buildingName);

    boolean existsByOwner_UserIdAndBuildingNameIgnoreCaseAndBuildingIdNot(Integer ownerId, String buildingName, Integer buildingId);

    boolean existsByOwner_UserId(Integer userId);

    List<Building> findAllByOrderByBuildingNameAsc();
}
