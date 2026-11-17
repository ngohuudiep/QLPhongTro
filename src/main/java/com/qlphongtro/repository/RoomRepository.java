package com.qlphongtro.repository;

import com.qlphongtro.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {

    Optional<Room> findByRoomCode(String roomCode);

    List<Room> findByBuilding_BuildingId(Integer buildingId);

    List<Room> findByBuilding_BuildingIdIn(List<Integer> buildingIds);

    java.util.Optional<Room> findByRoomIdAndBuilding_BuildingIdIn(Integer roomId, List<Integer> buildingIds);

    List<Room> findByStatus(String status);
}
