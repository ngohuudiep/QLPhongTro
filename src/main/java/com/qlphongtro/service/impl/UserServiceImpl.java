package com.qlphongtro.service.impl;

import com.qlphongtro.dto.ProfileDto;
import com.qlphongtro.dto.RegisterRequest;
import com.qlphongtro.entity.Role;
import com.qlphongtro.entity.User;
import com.qlphongtro.repository.RoleRepository;
import com.qlphongtro.repository.UserRepository;
import com.qlphongtro.service.OtpDeliveryException;
import com.qlphongtro.service.OtpSender;
import com.qlphongtro.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final int MAX_RESET_OTP_REQUESTS_PER_HOUR = 3;
    private static final int MAX_RESET_OTP_FAILED_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpSender otpSender;

    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            OtpSender otpSender) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpSender = otpSender;
    }

    @Override
    @Transactional
    public User registerUser(RegisterRequest request) {
        // 1. Kiểm tra 2 mật khẩu có khớp không
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu và xác nhận mật khẩu không khớp!");
        }

        // 2. Kiểm tra tên đăng nhập đã tồn tại chưa
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new IllegalArgumentException("Tên đăng nhập '" + request.getUsername() + "' đã được sử dụng!");
        }

        // 3. Kiểm tra email đã tồn tại chưa
        if (userRepository.existsByEmail(request.getEmail().trim())) {
            throw new IllegalArgumentException("Email '" + request.getEmail() + "' đã được đăng ký tài khoản khác!");
        }

        // 4. Kiểm tra SĐT đã tồn tại chưa
        if (userRepository.existsByPhone(request.getPhone().trim())) {
            throw new IllegalArgumentException("Số điện thoại '" + request.getPhone() + "' đã được đăng ký tài khoản khác!");
        }

        // Public registration always creates a tenant account.
        Role role = roleRepository.findByRoleName("USER")
                .orElseGet(() -> roleRepository.save(new Role(null, "USER")));

        // 6. Tạo User mới và băm mật khẩu bằng BCrypt
        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail().trim());
        user.setPhone(request.getPhone().trim());
        user.setRole(role);
        user.setFailedLoginAttempts(0);

        return userRepository.save(user);
    }

    @Override
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng: " + username));
    }

    @Override
    @Transactional
    public void updateProfile(String username, ProfileDto.UpdateInfo dto) {
        User user = getByUsername(username);

        // Kiểm tra email nếu thay đổi
        if (!dto.getEmail().trim().equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(dto.getEmail().trim())) {
            throw new IllegalArgumentException("Email '" + dto.getEmail() + "' đã được sử dụng!");
        }

        // Kiểm tra SĐT nếu thay đổi
        if (!dto.getPhone().trim().equalsIgnoreCase(user.getPhone()) && userRepository.existsByPhone(dto.getPhone().trim())) {
            throw new IllegalArgumentException("Số điện thoại '" + dto.getPhone() + "' đã được sử dụng!");
        }

        user.setFullName(dto.getFullName().trim());
        user.setEmail(dto.getEmail().trim());
        user.setPhone(dto.getPhone().trim());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(String username, ProfileDto.ChangePassword dto) {
        User user = getByUsername(username);

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác!");
        }

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới và xác nhận mật khẩu không khớp!");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void generateResetOtp(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return;
        }

        User user = userRepository.findByIdentifierForUpdate(identifier.trim()).orElse(null);
        if (user == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime requestWindowStart = user.getResetOtpRequestWindowStart();
        if (requestWindowStart == null || !requestWindowStart.plusHours(1).isAfter(now)) {
            user.setResetOtpRequestWindowStart(now);
            user.setResetOtpRequestCount(0);
        }
        if (user.getResetOtpRequestCount() >= MAX_RESET_OTP_REQUESTS_PER_HOUR) {
            return;
        }

        user.setResetOtpRequestCount(user.getResetOtpRequestCount() + 1);
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            userRepository.save(user);
            return;
        }

        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        user.setResetOtpHash(passwordEncoder.encode(otp));
        user.setResetOtpExpiry(now.plusMinutes(10));
        user.setResetOtpFailedAttempts(0);
        userRepository.save(user);
        try {
            otpSender.send(user.getEmail(), otp);
        } catch (OtpDeliveryException exception) {
            clearResetOtp(user);
            userRepository.save(user);
            logger.error("Password reset email delivery failed.");
        }
    }

    @Override
    @Transactional
    public boolean resetPasswordWithOtp(String identifier, String otp, String newPassword) {
        if (identifier == null || identifier.isBlank() || otp == null || otp.isBlank()) {
            return false;
        }

        User user = userRepository.findByIdentifierForUpdate(identifier.trim()).orElse(null);
        if (user == null || user.getResetOtpHash() == null) {
            return false;
        }

        LocalDateTime expiry = user.getResetOtpExpiry();
        if (expiry == null || !expiry.isAfter(LocalDateTime.now())) {
            clearResetOtp(user);
            userRepository.save(user);
            return false;
        }

        if (user.getResetOtpFailedAttempts() >= MAX_RESET_OTP_FAILED_ATTEMPTS) {
            clearResetOtp(user);
            userRepository.save(user);
            return false;
        }

        String otpHash = user.getResetOtpHash();
        if (!otpHash.matches("^\\$2[aby]\\$.{56}$") || !passwordEncoder.matches(otp.trim(), otpHash)) {
            int failedAttempts = user.getResetOtpFailedAttempts() + 1;
            user.setResetOtpFailedAttempts(failedAttempts);
            if (failedAttempts >= MAX_RESET_OTP_FAILED_ATTEMPTS) {
                clearResetOtp(user);
            }
            userRepository.save(user);
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        clearResetOtp(user);
        user.setFailedLoginAttempts(0);
        user.setLockoutUntil(null);
        userRepository.save(user);
        return true;
    }

    private void clearResetOtp(User user) {
        user.setResetOtpHash(null);
        user.setResetOtpExpiry(null);
        user.setResetOtpFailedAttempts(0);
    }
}
