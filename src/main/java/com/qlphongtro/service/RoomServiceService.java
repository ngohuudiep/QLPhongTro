package com.qlphongtro.service;

import com.qlphongtro.entity.RoomService;
import java.util.List;

public interface RoomServiceService {

    List<RoomService> getServicesByRoomId(Integer roomId);

    RoomService getRoomServiceById(Integer priceId);

    RoomService assignServiceToRoom(RoomService roomService);

    void removeServiceFromRoom(Integer priceId);
}
