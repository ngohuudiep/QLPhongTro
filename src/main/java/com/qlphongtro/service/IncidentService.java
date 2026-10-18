package com.qlphongtro.service;

import com.qlphongtro.entity.Incident;
import java.util.List;

public interface IncidentService {

    List<Incident> getAllIncidents();

    Incident getIncidentById(Integer id);

    List<Incident> getIncidentsByRoomId(Integer roomId);

    Incident saveIncident(Incident incident);

    void deleteIncident(Integer id);
}
