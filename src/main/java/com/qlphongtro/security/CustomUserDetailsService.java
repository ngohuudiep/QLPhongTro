package com.qlphongtro.security;

import com.qlphongtro.entity.Role;
import com.qlphongtro.entity.User;
import com.qlphongtro.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation của UserDetailsService cho Spring Security.
 * Hỗ trợ đăng nhập linh hoạt bằng Username, Email hoặc Số điện thoại.
 * Tích hợp kiểm tra tạm khóa tài khoản (Rate Limit / Brute-force protection),
 * kiểm tra trạng thái phê duyệt của Chủ trọ và gán Role tương thích cả RBAC cũ và mới.
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
        User user = userRepository.findByIdentifier(identifier.trim())
                .orElseThrow(() -> new UsernameNotFoundException("Thông tin đăng nhập không chính xác"));

        if (user.isAccountLocked()) {
            throw new LockedException("Tài khoản đang bị tạm khóa do nhập sai mật khẩu nhiều lần. Vui lòng thử lại sau 15 phút.");
        }

        if (!user.isEnabled()) {
            throw new DisabledException("Tài khoản đã bị quản trị viên khóa.");
        }

        String rawRole = user.getRole() != null ? user.getRole().getRoleName() : null;
        String normalizedRole = Role.normalizeRoleName(rawRole);

        if (!List.of(Role.ADMIN, Role.LANDLORD, Role.TENANT).contains(normalizedRole)) {
            throw new DisabledException("Tài khoản chưa được gán vai trò hợp lệ.");
        }

        if (Role.LANDLORD.equals(normalizedRole)) {
            if ("PENDING_APPROVAL".equalsIgnoreCase(user.getAccountStatus())
                    || "PENDING".equalsIgnoreCase(user.getAccountStatus())) {
                throw new DisabledException("Tài khoản chủ trọ đang chờ Admin phê duyệt.");
            }
            if ("REJECTED".equalsIgnoreCase(user.getAccountStatus())) {
                String reason = user.getRejectionReason();
                throw new DisabledException(reason == null || reason.isBlank()
                        ? "Đăng ký chủ trọ đã bị từ chối. Vui lòng liên hệ Admin."
                        : "Đăng ký chủ trọ đã bị từ chối. Lý do: " + reason);
            }
            if (!user.isApproved() && !"ACTIVE".equalsIgnoreCase(user.getAccountStatus())) {
                throw new DisabledException("Tài khoản chủ trọ chưa được phê duyệt.");
            }
        }

        if (Role.TENANT.equals(normalizedRole) && !user.isActiveTenant()) {
            throw new DisabledException("Tài khoản người thuê không ở trạng thái hoạt động.");
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (Role.ADMIN.equals(normalizedRole)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else if (Role.LANDLORD.equals(normalizedRole) || Role.STAFF.equalsIgnoreCase(rawRole)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_LANDLORD"));
            authorities.add(new SimpleGrantedAuthority("ROLE_STAFF"));
        } else if (Role.TENANT.equals(normalizedRole) || Role.USER.equalsIgnoreCase(rawRole)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_TENANT"));
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            authorities.add(new SimpleGrantedAuthority("ROLE_NGUOITHUE"));
            authorities.add(new SimpleGrantedAuthority("ROLE_NGUOI_THUE"));
        } else if (rawRole != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + rawRole.toUpperCase()));
        }

        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountLocked(user.isAccountLocked())
                .disabled(!user.isEnabled())
                .build();
    }
}
