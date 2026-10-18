package com.qlphongtro.repository;

import com.qlphongtro.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Integer> {

    // Tìm kiếm theo số CCCD/CMND
    Optional<Tenant> findByIdNumber(String idNumber);

    // Kiểm tra CCCD đã tồn tại chưa
    boolean existsByIdNumber(String idNumber);

    // Tìm theo số điện thoại
    Optional<Tenant> findByPhone(String phone);

    // Tìm kiếm khách thuê theo tên hoặc số CCCD hoặc SĐT
    List<Tenant> findByFullNameContainingIgnoreCaseOrIdNumberContainingOrPhoneContaining(
            String fullName, String idNumber, String phone
    );
}
