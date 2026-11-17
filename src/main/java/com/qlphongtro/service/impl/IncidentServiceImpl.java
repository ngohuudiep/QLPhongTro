package com.qlphongtro.service.impl;

import com.qlphongtro.entity.Incident;
import com.qlphongtro.entity.Tenant;
import com.qlphongtro.repository.ContractRepository;
import com.qlphongtro.repository.IncidentRepository;
import com.qlphongtro.repository.TenantRepository;
import com.qlphongtro.security.CurrentUser;
import com.qlphongtro.service.IncidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;
    private final TenantRepository tenantRepository;
    private final ContractRepository contractRepository;

    @Autowired
    public IncidentServiceImpl(IncidentRepository incidentRepository, TenantRepository tenantRepository,
                               ContractRepository contractRepository) {
        this.incidentRepository = incidentRepository;
        this.tenantRepository = tenantRepository;
        this.contractRepository = contractRepository;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    @Transactional(readOnly = true)
    public List<Incident> getAllIncidents() {
        if (CurrentUser.isTenant()) {
            return incidentRepository.findAllForTenant(CurrentUser.username());
        }
        return incidentRepository.findAll();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    @Transactional(readOnly = true)
    public Incident getIncidentById(Integer id) {
        if (CurrentUser.isTenant()) {
            return incidentRepository.findByIdAndTenantUsername(id, CurrentUser.username())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        }
        return incidentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sự cố ID: " + id));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    @Transactional(readOnly = true)
    public List<Incident> getIncidentsByRoomId(Integer roomId) {
        if (CurrentUser.isTenant()) {
            return incidentRepository.findByRoomIdAndTenantUsername(roomId, CurrentUser.username());
        }
        return incidentRepository.findByRoom_RoomId(roomId);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Incident saveIncident(Incident incident) {
        if (CurrentUser.isTenant()) {
            Tenant tenant = tenantRepository.findByUser_Username(CurrentUser.username())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (incident.getIncidentId() == null) {
                Integer roomId = incident.getRoom() == null ? null : incident.getRoom().getRoomId();
                if (roomId == null || !contractRepository.existsForTenantAndRoom(CurrentUser.username(), roomId)) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                }
                incident.setReportedBy(tenant);
                incident.setStatus("Ti\u1ebfp nh\u1eadn");
            } else {
                Incident existing = incidentRepository.findByIdAndTenantUsername(incident.getIncidentId(), CurrentUser.username())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                if (!"Ti\u1ebfp nh\u1eadn".equals(existing.getStatus())) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                }
                incident.setRoom(existing.getRoom());
                incident.setReportedBy(existing.getReportedBy());
                incident.setStatus(existing.getStatus());
                incident.setResolvedAt(existing.getResolvedAt());
                incident.setReportedAt(existing.getReportedAt());
            }
        }
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
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteIncident(Integer id) {
        if (!incidentRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy sự cố ID: " + id);
        }
        incidentRepository.deleteById(id);
    }
}
