package com.qlphongtro.repository;

import com.qlphongtro.entity.RoomService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomServiceRepository extends JpaRepository<RoomService, Integer> {

    // Tìm tất cả dịch vụ đang được gán cho một phòng
    List<RoomService> findByRoom_RoomId(Integer roomId);

    // Tìm tất cả các phòng đang sử dụng một dịch vụ cụ thể
    List<RoomService> findByService_ServiceId(Integer serviceId);
}
