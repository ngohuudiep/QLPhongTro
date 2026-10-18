package com.qlphongtro.security;

import com.qlphongtro.entity.User;
import com.qlphongtro.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation của UserDetailsService cho Spring Security.
 * Khi người dùng đăng nhập, Spring Security sẽ gọi loadUserByUsername()
 * để lấy thông tin tài khoản và quyền từ database.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Tải thông tin người dùng từ DB theo username.
     * Được Spring Security gọi tự động khi người dùng submit form đăng nhập.
     *
     * @param username tên đăng nhập từ form login
     * @return UserDetails với thông tin xác thực và danh sách quyền
     * @throws UsernameNotFoundException nếu không tìm thấy tài khoản
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Tìm người dùng trong DB - ném exception nếu không tồn tại
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Không tìm thấy tài khoản: " + username));

        // Chuyển Role của user thành GrantedAuthority
        // Spring Security yêu cầu role prefix = "ROLE_" (vd: ROLE_ADMIN, ROLE_STAFF)
        String roleName = "ROLE_" + user.getRole().getRoleName();
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleName));

        // Trả về UserDetails chuẩn của Spring Security
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())       // Đã được mã hóa BCrypt trong DB
                .authorities(authorities)
                .build();
    }
}
