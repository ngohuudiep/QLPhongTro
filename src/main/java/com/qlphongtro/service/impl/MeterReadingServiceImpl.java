package com.qlphongtro.service.impl;

import com.qlphongtro.entity.MeterReading;
import com.qlphongtro.repository.MeterReadingRepository;
import com.qlphongtro.service.MeterReadingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class MeterReadingServiceImpl implements MeterReadingService {

    private final MeterReadingRepository meterReadingRepository;

    @Autowired
    public MeterReadingServiceImpl(MeterReadingRepository meterReadingRepository) {
        this.meterReadingRepository = meterReadingRepository;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    @Transactional(readOnly = true)
    public List<MeterReading> getAllReadings() {
        return meterReadingRepository.findAll();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    @Transactional(readOnly = true)
    public List<MeterReading> getReadingsByRoomId(Integer roomId) {
        return meterReadingRepository.findByRoom_RoomIdOrderByBillingMonthDesc(roomId);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'USER')")
    @Transactional(readOnly = true)
    public MeterReading getReadingById(Integer id) {
        return meterReadingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản ghi chỉ số ID: " + id));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public MeterReading recordReading(MeterReading reading) {
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
        return meterReadingRepository.findByRoom_RoomIdAndBillingMonth(roomId, billingMonth)
                .orElse(null);
    }
}
