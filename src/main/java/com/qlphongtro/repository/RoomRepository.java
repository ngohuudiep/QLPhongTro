package com.qlphongtro.repository;

import com.qlphongtro.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {

    Optional<Room> findByRoomCode(String roomCode);

    List<Room> findByBuilding_BuildingId(Integer buildingId);

    List<Room> findByBuilding_BuildingIdIn(List<Integer> buildingIds);

    Optional<Room> findByRoomIdAndBuilding_BuildingIdIn(Integer roomId, List<Integer> buildingIds);

    List<Room> findByStatus(String status);
    List<Room> findByBuilding_Owner_UsernameOrderByBuilding_BuildingNameAscRoomCodeAsc(String username);
    Optional<Room> findByRoomIdAndBuilding_Owner_Username(Integer roomId, String username);
    long countByBuilding_BuildingId(Integer buildingId);
    long countByStatus(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r JOIN FETCH r.building WHERE r.roomId = :roomId")
    Optional<Room> findByIdForUpdate(@Param("roomId") Integer roomId);

    @Query("SELECT r FROM Room r JOIN FETCH r.building b LEFT JOIN FETCH b.owner " +
           "WHERE (:ownerId IS NULL OR b.owner.userId = :ownerId) " +
           "AND (:status IS NULL OR UPPER(r.status) = UPPER(:status)) " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(r.roomCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.buildingName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.address) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY b.buildingName, r.roomCode")
    List<Room> searchAllForAdmin(@Param("keyword") String keyword,
                                 @Param("status") String status,
                                 @Param("ownerId") Integer ownerId);

    @Query("SELECT r FROM Room r WHERE r.building.owner.username = :username " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(r.roomCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.building.buildingName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.building.address) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:status IS NULL OR :status = '' OR r.status = :status) " +
           "ORDER BY r.building.buildingName, r.roomCode")
    List<Room> findOwnedRooms(@Param("username") String username,
                              @Param("keyword") String keyword,
                              @Param("status") String status);
}