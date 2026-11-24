package com.qlphongtro.dto;

import java.time.LocalDate;

public class TenantAccountOption {

    private final Integer userId;
    private final String username;
    private final String fullName;
    private final String phone;
    private final String email;
    private final Integer tenantId;
    private final String idNumber;
    private final String hometown;
    private final LocalDate dateOfBirth;

    public TenantAccountOption(Integer userId, String username, String fullName, String phone, String email,
                               Integer tenantId, String idNumber, String hometown, LocalDate dateOfBirth) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.tenantId = tenantId;
        this.idNumber = idNumber;
        this.hometown = hometown;
        this.dateOfBirth = dateOfBirth;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public Integer getTenantId() {
        return tenantId;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public String getHometown() {
        return hometown;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
}
