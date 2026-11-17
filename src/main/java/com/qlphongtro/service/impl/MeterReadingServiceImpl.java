package com.qlphongtro.service.impl;

import com.qlphongtro.entity.MeterReading;
import com.qlphongtro.repository.MeterReadingRepository;
import com.qlphongtro.security.CurrentUser;
import com.qlphongtro.security.BuildingAccess;
import com.qlphongtro.service.MeterReadingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class MeterReadingServiceImpl implements MeterReadingService {

    private final MeterReadingRepository meterReadingRepository;
    private final BuildingAccess buildingAccess;

    @Autowired
    public MeterReadingServiceImpl(MeterReadingRepository meterReadingRepository, BuildingAccess buildingAccess) {
        this.meterReadingRepository = meterReadingRepository;
        this.buildingAccess = buildingAccess;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    @Transactional(readOnly = true)
    public List<MeterReading> getAllReadings() {
        if (CurrentUser.isTenant()) {
            return meterReadingRepository.findAllForTenant(CurrentUser.username());
        }
        if (buildingAccess.isStaff()) {
            List<Integer> buildingIds = buildingAccess.assignedBuildingIds();
            return buildingIds.isEmpty() ? List.of() : meterReadingRepository.findByRoom_Building_BuildingIdIn(buildingIds);
        }
        return meterReadingRepository.findAll();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    @Transactional(readOnly = true)
    public List<MeterReading> getReadingsByRoomId(Integer roomId) {
        if (CurrentUser.isTenant()) {
            return meterReadingRepository.findByRoomIdAndTenantUsername(roomId, CurrentUser.username());
        }
        if (buildingAccess.isStaff()) {
            List<Integer> buildingIds = buildingAccess.assignedBuildingIds();
            return buildingIds.isEmpty() ? List.of() : meterReadingRepository.findByRoom_RoomIdAndRoom_Building_BuildingIdIn(roomId, buildingIds);
        }
        return meterReadingRepository.findByRoom_RoomIdOrderByBillingMonthDesc(roomId);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    @Transactional(readOnly = true)
    public MeterReading getReadingById(Integer id) {
        if (CurrentUser.isTenant()) {
            return meterReadingRepository.findByIdAndTenantUsername(id, CurrentUser.username())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        }
        if (buildingAccess.isStaff()) {
            List<Integer> buildingIds = buildingAccess.assignedBuildingIds();
            if (buildingIds.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            return meterReadingRepository.findByReadingIdAndRoom_Building_BuildingIdIn(id, buildingIds)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        }
        return meterReadingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản ghi chỉ số ID: " + id));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public MeterReading recordReading(MeterReading reading) {
        if (buildingAccess.isStaff()) {
            buildingAccess.requireRoom(reading.getRoom());
            if (reading.getReadingId() != null) {
                getReadingById(reading.getReadingId());
            }
        }
        // Tự động tính chỉ số tiêu thụ nếu chưa có
        if (reading.getStartIndex() != null && reading.getEndIndex() != null) {
            reading.setConsumption(reading.getEndIndex().subtract(reading.getStartIndex()));
        }
        return meterReadingRepository.save(reading);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteReading(Integer id) {
        if (!meterReadingRepository.existsById(id)) {
            throw new RuntimeException("Không thể xóa, bản ghi chỉ số không tồn tại ID: " + id);
        }
        meterReadingRepository.deleteById(id);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    @Transactional(readOnly = true)
    public MeterReading getReadingByRoomAndMonth(Integer roomId, LocalDate billingMonth) {
        if (CurrentUser.isTenant()) {
            return meterReadingRepository.findByRoomIdAndBillingMonthAndTenantUsername(roomId, billingMonth, CurrentUser.username())
                    .orElse(null);
        }
        if (buildingAccess.isStaff()) {
            List<Integer> buildingIds = buildingAccess.assignedBuildingIds();
            return buildingIds.isEmpty() ? null : meterReadingRepository.findByRoom_RoomIdAndBillingMonthAndRoom_Building_BuildingIdIn(roomId, billingMonth, buildingIds).orElse(null);
        }
        return meterReadingRepository.findByRoom_RoomIdAndBillingMonth(roomId, billingMonth)
                .orElse(null);
    }
}
