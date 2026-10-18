package com.qlphongtro.service;

import com.qlphongtro.entity.Room;
import java.util.List;

public interface RoomService {

    List<Room> getAllRooms();

    Room getRoomById(Integer id);

    Room saveRoom(Room room);

    void deleteRoom(Integer id);
}
