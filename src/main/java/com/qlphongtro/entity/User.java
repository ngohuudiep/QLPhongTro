package com.qlphongtro.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Entity ánh xạ bảng Users trong SQL Server.
 * Quản lý tài khoản đăng nhập (Chủ trọ / Người thuê / Nhân viên).
 */
@Entity
@Table(name = "Users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    /** Tên đăng nhập (username) */
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(max = 50, message = "Tên đăng nhập tối đa 50 ký tự")
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /** Mật khẩu đã băm BCrypt */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /** Họ và tên hiển thị (Hỗ trợ tiếng Việt Unicode) */
    @Size(max = 150, message = "Họ và tên tối đa 150 ký tự")
    @Column(name = "full_name", columnDefinition = "NVARCHAR(150)")
    private String fullName;

    /** Email liên hệ & đăng nhập */
    @Column(name = "email", length = 100, unique = true)
    private String email;

    /** Số điện thoại liên hệ & đăng nhập */
    @Column(name = "phone", length = 20, unique = true)
    private String phone;

    /** Số lần đăng nhập sai liên tiếp để rate limit / lockout */
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    /** Thời gian mở khóa nếu bị tạm khóa do đăng nhập sai nhiều lần */
    @Column(name = "lockout_until")
    private LocalDateTime lockoutUntil;

    /** Mã OTP khôi phục mật khẩu */
    @Column(name = "reset_otp", length = 10)
    private String resetOtp;

    /** Thời gian hết hạn của OTP */
    @Column(name = "reset_otp_expiry")
    private LocalDateTime resetOtpExpiry;

    /** Vai trò người dùng (ADMIN: Chủ trọ, USER: Người thuê, STAFF: Nhân viên) */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    public User() {}

    public User(Integer userId, String username, String password, String fullName, String email, String phone, Role role) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.failedLoginAttempts = 0;
    }

    // ==================== Getters & Setters ====================

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getFailedLoginAttempts() { return failedLoginAttempts != null ? failedLoginAttempts : 0; }
    public void setFailedLoginAttempts(Integer failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public LocalDateTime getLockoutUntil() { return lockoutUntil; }
    public void setLockoutUntil(LocalDateTime lockoutUntil) { this.lockoutUntil = lockoutUntil; }

    public String getResetOtp() { return resetOtp; }
    public void setResetOtp(String resetOtp) { this.resetOtp = resetOtp; }

    public LocalDateTime getResetOtpExpiry() { return resetOtpExpiry; }
    public void setResetOtpExpiry(LocalDateTime resetOtpExpiry) { this.resetOtpExpiry = resetOtpExpiry; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isAccountLocked() {
        if (lockoutUntil == null) return false;
        return lockoutUntil.isAfter(LocalDateTime.now());
    }

    @Override
    public String toString() {
        return "User{userId=" + userId + ", username='" + username + "', fullName='" + fullName + "', email='" + email + "'}";
    }
}
