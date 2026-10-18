package com.qlphongtro.service;

import com.qlphongtro.entity.MeterReading;
import java.time.LocalDate;
import java.util.List;

public interface MeterReadingService {

    List<MeterReading> getAllReadings();

    List<MeterReading> getReadingsByRoomId(Integer roomId);

    MeterReading getReadingById(Integer id);

    MeterReading recordReading(MeterReading reading);

    void deleteReading(Integer id);

    MeterReading getReadingByRoomAndMonth(Integer roomId, LocalDate billingMonth);
}
