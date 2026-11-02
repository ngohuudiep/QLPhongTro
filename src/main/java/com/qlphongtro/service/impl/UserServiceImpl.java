package com.qlphongtro.service.impl;

import com.qlphongtro.dto.ProfileDto;
import com.qlphongtro.dto.RegisterRequest;
import com.qlphongtro.entity.Role;
import com.qlphongtro.entity.User;
import com.qlphongtro.repository.RoleRepository;
import com.qlphongtro.repository.UserRepository;
import com.qlphongtro.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
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

        // 5. Xác định vai trò: "ADMIN" (Chủ trọ) hoặc "USER" (Người thuê)
        String targetRole = "ADMIN".equalsIgnoreCase(request.getRoleChoice()) ? "ADMIN" : "USER";
        Role role = roleRepository.findByRoleName(targetRole)
                .orElseGet(() -> roleRepository.save(new Role(null, targetRole)));

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
    public String generateResetOtp(String identifier) {
        User user = userRepository.findByIdentifier(identifier.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với thông tin đã cung cấp."));

        // Tạo mã OTP 6 số ngẫu nhiên
        SecureRandom random = new SecureRandom();
        String otp = String.format("%06d", random.nextInt(1000000));

        user.setResetOtp(otp);
        user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        return otp;
    }

    @Override
    @Transactional
    public void resetPasswordWithOtp(String identifier, String otp, String newPassword) {
        User user = userRepository.findByIdentifier(identifier.trim())
                .orElseThrow(() -> new IllegalArgumentException("Thông tin tài khoản không hợp lệ."));

        if (user.getResetOtp() == null || !user.getResetOtp().equals(otp.trim())) {
            throw new IllegalArgumentException("Mã OTP không chính xác!");
        }

        if (user.getResetOtpExpiry() == null || user.getResetOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Mã OTP đã hết hiệu lực. Vui lòng yêu cầu mã mới!");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetOtp(null);
        user.setResetOtpExpiry(null);
        user.setFailedLoginAttempts(0);
        user.setLockoutUntil(null);
        userRepository.save(user);
    }
}
