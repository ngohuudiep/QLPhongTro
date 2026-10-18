package com.qlphongtro.repository;

import com.qlphongtro.entity.MeterReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeterReadingRepository extends JpaRepository<MeterReading, Integer> {

    // Lấy lịch sử chỉ số của một phòng sắp xếp theo tháng giảm dần
    List<MeterReading> findByRoom_RoomIdOrderByBillingMonthDesc(Integer roomId);

    // Tìm chỉ số của phòng theo tháng cụ thể
    Optional<MeterReading> findByRoom_RoomIdAndBillingMonth(Integer roomId, LocalDate billingMonth);
}
