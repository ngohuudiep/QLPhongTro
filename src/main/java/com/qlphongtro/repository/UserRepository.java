package com.qlphongtro.repository;

import com.qlphongtro.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

/**
 * Repository thao tác dữ liệu bảng Users.
 * Hỗ trợ tìm kiếm theo Username, Email, hoặc Số điện thoại.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.username = :username")
    Optional<User> findByUsername(@Param("username") String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.username = :identifier OR u.email = :identifier OR u.phone = :identifier")
    Optional<User> findByIdentifierForUpdate(@Param("identifier") String identifier);

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.phone = :phone")
    Optional<User> findByPhone(@Param("phone") String phone);

    /**
     * Tìm người dùng qua Username, Email hoặc Số điện thoại (dùng cho form đăng nhập đa năng)
     */
    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.username = :identifier OR u.email = :identifier OR u.phone = :identifier")
    Optional<User> findByIdentifier(@Param("identifier") String identifier);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    List<User> findByRole_RoleId(Integer roleId);

    List<User> findByRole_RoleName(String roleName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.role.roleName = 'ADMIN' AND u.accountEnabled = true ORDER BY u.userId")
    List<User> findActiveAdministratorsForUpdate();
    boolean existsByEmailAndUserIdNot(String email, Integer userId);
    boolean existsByPhoneAndUserIdNot(String phone, Integer userId);
    long countByRole_RoleNameIgnoreCase(String roleName);
    long countByRole_RoleNameIgnoreCaseAndAccountStatusIgnoreCase(String roleName, String accountStatus);
    List<User> findByRole_RoleNameIgnoreCaseAndAccountStatusIgnoreCaseOrderByRegisteredAtAsc(String roleName, String accountStatus);

    @Query("SELECT u FROM User u JOIN FETCH u.role r WHERE " +
           "UPPER(r.roleName) = UPPER(:roleName) " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY u.userId DESC")
    List<User> searchAccountsByRole(@Param("roleName") String roleName, @Param("keyword") String keyword);
}