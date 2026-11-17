package com.qlphongtro.service.impl;

import com.qlphongtro.entity.Room;
import com.qlphongtro.repository.RoomRepository;
import com.qlphongtro.security.BuildingAccess;
import com.qlphongtro.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final BuildingAccess buildingAccess;

    @Autowired
    public RoomServiceImpl(RoomRepository roomRepository, BuildingAccess buildingAccess) {
        this.roomRepository = roomRepository;
        this.buildingAccess = buildingAccess;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public List<Room> getAllRooms() {
        if (buildingAccess.isStaff()) {
            List<Integer> buildingIds = buildingAccess.assignedBuildingIds();
            return buildingIds.isEmpty() ? List.of() : roomRepository.findByBuilding_BuildingIdIn(buildingIds);
        }
        return roomRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    public Room getRoomById(Integer id) {
        if (buildingAccess.isStaff()) {
            List<Integer> buildingIds = buildingAccess.assignedBuildingIds();
            if (buildingIds.isEmpty()) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
            }
            return roomRepository.findByRoomIdAndBuilding_BuildingIdIn(id, buildingIds)
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        }
        return roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng có ID: " + id));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public Room saveRoom(Room room) {
        if (buildingAccess.isStaff()) {
            if (room.getRoomId() == null) {
                throw new org.springframework.security.access.AccessDeniedException("Staff can only update room status.");
            }
            Room existing = roomRepository.findByRoomIdAndBuilding_BuildingIdIn(
                            room.getRoomId(), buildingAccess.assignedBuildingIds())
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
            room.setRoomCode(existing.getRoomCode());
            room.setBuilding(existing.getBuilding());
            room.setFloorNo(existing.getFloorNo());
            room.setMaxOccupants(existing.getMaxOccupants());
            room.setPriceOverride(existing.getPriceOverride());
            room.setRoomServices(existing.getRoomServices());
            room.setMeterReadings(existing.getMeterReadings());
        }
        return roomRepository.save(room);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteRoom(Integer id) {
        if (!roomRepository.existsById(id)) {
            throw new RuntimeException("Không thể xóa, phòng không tồn tại ID: " + id);
        }
        roomRepository.deleteById(id);
    }
}
