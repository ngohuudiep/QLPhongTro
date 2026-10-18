package com.qlphongtro.repository;

import com.qlphongtro.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Integer> {

    List<Incident> findByRoom_RoomId(Integer roomId);

    List<Incident> findByPriority(String priority);

    List<Incident> findByStatus(String status);
}
