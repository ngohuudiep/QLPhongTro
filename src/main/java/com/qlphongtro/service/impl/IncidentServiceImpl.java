package com.qlphongtro.service.impl;

import com.qlphongtro.entity.Incident;
import com.qlphongtro.repository.IncidentRepository;
import com.qlphongtro.service.IncidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;

    @Autowired
    public IncidentServiceImpl(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Incident> getAllIncidents() {
        return incidentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Incident getIncidentById(Integer id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự cố ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Incident> getIncidentsByRoomId(Integer roomId) {
        return incidentRepository.findByRoom_RoomId(roomId);
    }

    @Override
    public Incident saveIncident(Incident incident) {
        if (incident.getReportedAt() == null) {
            incident.setReportedAt(LocalDateTime.now());
        }
        // Nếu chuyển trạng thái sang "Hoàn thành" mà chưa có thời gian giải quyết, tự động gán ngày giờ hiện tại
        if ("Hoàn thành".equalsIgnoreCase(incident.getStatus()) && incident.getResolvedAt() == null) {
            incident.setResolvedAt(LocalDateTime.now());
        }
        return incidentRepository.save(incident);
    }

    @Override
    public void deleteIncident(Integer id) {
        if (!incidentRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy sự cố ID: " + id);
        }
        incidentRepository.deleteById(id);
    }
}
