package com.qlphongtro.repository;

import com.qlphongtro.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Integer> {

    List<Incident> findByRoom_RoomId(Integer roomId);

    List<Incident> findByRoom_Building_BuildingIdIn(List<Integer> buildingIds);

    Optional<Incident> findByIncidentIdAndRoom_Building_BuildingIdIn(Integer incidentId, List<Integer> buildingIds);

    List<Incident> findByRoom_RoomIdAndRoom_Building_BuildingIdIn(Integer roomId, List<Integer> buildingIds);

    @Query("select i from Incident i left join i.reportedBy rb left join i.tenant t left join t.user tu where rb.username = :username or tu.username = :username")
    List<Incident> findAllForTenant(@Param("username") String username);

    @Query("select i from Incident i left join i.reportedBy rb left join i.tenant t left join t.user tu where i.incidentId = :incidentId and (rb.username = :username or tu.username = :username)")
    Optional<Incident> findByIdAndTenantUsername(@Param("incidentId") Integer incidentId, @Param("username") String username);

    @Query("select i from Incident i left join i.reportedBy rb left join i.tenant t left join t.user tu where i.room.roomId = :roomId and (rb.username = :username or tu.username = :username)")
    List<Incident> findByRoomIdAndTenantUsername(@Param("roomId") Integer roomId, @Param("username") String username);

    List<Incident> findByPriority(String priority);

    List<Incident> findByStatus(String status);
    List<Incident> findByReportedBy_UserIdOrderByReportedAtDesc(Integer userId);
}