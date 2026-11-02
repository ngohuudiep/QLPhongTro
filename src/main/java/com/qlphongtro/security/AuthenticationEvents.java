package com.qlphongtro.security;

import com.qlphongtro.entity.User;
import com.qlphongtro.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Lắng nghe sự kiện đăng nhập của Spring Security:
 *  - Xử lý Rate Limiting: Sau 5 lần nhập sai, tạm khóa tài khoản 15 phút.
 *  - Khi đăng nhập thành công: Reset bộ đếm số lần sai về 0.
 */
@Component
public class AuthenticationEvents {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_TIME_MINUTES = 15;

    private final UserRepository userRepository;

    @Autowired
    public AuthenticationEvents(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        if (principal instanceof String identifier) {
            userRepository.findByIdentifier(identifier.trim()).ifPresent(user -> {
                int attempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(attempts);
                if (attempts >= MAX_FAILED_ATTEMPTS) {
                    user.setLockoutUntil(LocalDateTime.now().plusMinutes(LOCK_TIME_MINUTES));
                }
                userRepository.save(user);
            });
        }
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            userRepository.findByUsername(userDetails.getUsername()).ifPresent(user -> {
                if (user.getFailedLoginAttempts() > 0 || user.getLockoutUntil() != null) {
                    user.setFailedLoginAttempts(0);
                    user.setLockoutUntil(null);
                    userRepository.save(user);
                }
            });
        }
    }
}
