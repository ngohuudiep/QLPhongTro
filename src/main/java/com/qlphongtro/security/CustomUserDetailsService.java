package com.qlphongtro.security;

import com.qlphongtro.entity.User;
import com.qlphongtro.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation của UserDetailsService cho Spring Security.
 * Hỗ trợ đăng nhập linh hoạt bằng Username, Email hoặc Số điện thoại.
 * Tích hợp kiểm tra tạm khóa tài khoản (Rate Limit / Brute-force protection).
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // Tìm người dùng theo Username, Email hoặc SĐT
        User user = userRepository.findByIdentifier(identifier.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Thông tin đăng nhập không chính xác"));

        // Kiểm tra xem tài khoản có đang bị tạm khóa (Rate limit) không
        if (user.isAccountLocked()) {
            throw new LockedException("Tài khoản đang bị tạm khóa do nhập sai mật khẩu nhiều lần. Vui lòng thử lại sau 15 phút.");
        }

        // Quyền theo Role (ADMIN, USER, STAFF)
        String roleName = "ROLE_" + user.getRole().getRoleName();
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleName));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountLocked(user.isAccountLocked())
                .build();
    }
}
