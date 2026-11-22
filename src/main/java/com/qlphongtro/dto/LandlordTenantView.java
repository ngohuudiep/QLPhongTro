package com.qlphongtro.dto;

import java.time.LocalDate;

public record LandlordTenantView(
        Integer userId,
        String username,
        String fullName,
        String phone,
        String email,
        String idNumber,
        String roomCode,
        String contractCode,
        LocalDate startDate,
        LocalDate endDate,
        String contractStatus,
        String contractStatusLabel,
        Integer contractId) {
}
