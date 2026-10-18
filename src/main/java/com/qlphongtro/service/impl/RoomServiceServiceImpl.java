package com.qlphongtro.service.impl;

import com.qlphongtro.entity.RoomService;
import com.qlphongtro.repository.RoomServiceRepository;
import com.qlphongtro.service.RoomServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RoomServiceServiceImpl implements RoomServiceService {

    private final RoomServiceRepository roomServiceRepository;

    @Autowired
    public RoomServiceServiceImpl(RoomServiceRepository roomServiceRepository) {
        this.roomServiceRepository = roomServiceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomService> getServicesByRoomId(Integer roomId) {
        return roomServiceRepository.findByRoom_RoomId(roomId);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomService getRoomServiceById(Integer priceId) {
        return roomServiceRepository.findById(priceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình giá dịch vụ ID: " + priceId));
    }

    @Override
    public RoomService assignServiceToRoom(RoomService roomService) {
        return roomServiceRepository.save(roomService);
    }

    @Override
    public void removeServiceFromRoom(Integer priceId) {
        if (!roomServiceRepository.existsById(priceId)) {
            throw new RuntimeException("Không thể xóa, cấu hình giá dịch vụ không tồn tại ID: " + priceId);
        }
        roomServiceRepository.deleteById(priceId);
    }
}
