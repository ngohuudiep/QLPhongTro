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

    @Query("select i from Incident i where i.reportedBy.user.username = :username")
    List<Incident> findAllForTenant(@Param("username") String username);

    @Query("select i from Incident i where i.incidentId = :incidentId and i.reportedBy.user.username = :username")
    Optional<Incident> findByIdAndTenantUsername(@Param("incidentId") Integer incidentId, @Param("username") String username);

    @Query("select i from Incident i where i.room.roomId = :roomId and i.reportedBy.user.username = :username")
    List<Incident> findByRoomIdAndTenantUsername(@Param("roomId") Integer roomId, @Param("username") String username);

    List<Incident> findByPriority(String priority);

    List<Incident> findByStatus(String status);
}
