package com.qlphongtro.repository;

import com.qlphongtro.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository thao tác dữ liệu bảng Roles.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    /** Tìm Role theo tên (ADMIN, STAFF, USER) */
    Optional<Role> findByRoleName(String roleName);
}
