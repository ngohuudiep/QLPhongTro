package com.qlphongtro.repository;

import com.qlphongtro.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository thao tác dữ liệu bảng Users.
 * Spring Security sẽ dùng findByUsername để xác thực đăng nhập.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * Tìm User theo username - dùng trong quá trình đăng nhập (Spring Security).
     * EAGER load kèm Role để tránh LazyInitializationException khi đọc quyền.
     */
    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.username = :username")
    Optional<User> findByUsername(String username);

    /** Kiểm tra username đã tồn tại chưa - dùng khi tạo tài khoản mới */
    boolean existsByUsername(String username);

    /** Lấy tất cả user theo role_id */
    List<User> findByRole_RoleId(Integer roleId);

    /** Lấy tất cả user theo tên role (ADMIN, STAFF, USER) */
    List<User> findByRole_RoleName(String roleName);
}
