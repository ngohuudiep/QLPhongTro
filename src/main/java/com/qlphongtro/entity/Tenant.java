package com.qlphongtro.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Entity
@Table(name = "Tenants")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tenant_id")
    private Integer tenantId;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 150, message = "Họ và tên tối đa 150 ký tự")
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @NotBlank(message = "Số CCCD/CMND không được để trống")
    @Size(max = 20, message = "Số CCCD/CMND tối đa 20 ký tự")
    @Column(name = "id_number", nullable = false, length = 20, unique = true)
    private String idNumber;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không hợp lệ (gồm 10 số, bắt đầu bằng 0 hoặc +84)")
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Size(max = 255, message = "Quê quán tối đa 255 ký tự")
    @Column(name = "hometown", length = 255)
    private String hometown;

    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Email(message = "Địa chỉ email không đúng định dạng")
    @Size(max = 150, message = "Email tối đa 150 ký tự")
    @Column(name = "email", length = 150)
    private String email;

    public Tenant() {
    }

    public Tenant(Integer tenantId, String fullName, String idNumber, String phone, String hometown, LocalDate dateOfBirth, String email) {
        this.tenantId = tenantId;
        this.fullName = fullName;
        this.idNumber = idNumber;
        this.phone = phone;
        this.hometown = hometown;
        this.dateOfBirth = dateOfBirth;
        this.email = email;
    }

    public Integer getTenantId() {
        return tenantId;
    }

    public void setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getHometown() {
        return hometown;
    }

    public void setHometown(String hometown) {
        this.hometown = hometown;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public static class TenantBuilder {
        private Integer tenantId;
        private String fullName;
        private String idNumber;
        private String phone;
        private String hometown;
        private LocalDate dateOfBirth;
        private String email;

        TenantBuilder() {}

        public TenantBuilder tenantId(Integer tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public TenantBuilder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public TenantBuilder idNumber(String idNumber) {
            this.idNumber = idNumber;
            return this;
        }

        public TenantBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public TenantBuilder hometown(String hometown) {
            this.hometown = hometown;
            return this;
        }

        public TenantBuilder dateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public TenantBuilder email(String email) {
            this.email = email;
            return this;
        }

        public Tenant build() {
            return new Tenant(tenantId, fullName, idNumber, phone, hometown, dateOfBirth, email);
        }
    }

    public static TenantBuilder builder() {
        return new TenantBuilder();
    }
}
