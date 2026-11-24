package com.qlphongtro.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity ánh xạ bảng Users trong SQL Server.
 * Quản lý thông tin tài khoản đăng nhập cho toàn bộ hệ thống.
 */
@Entity
@Table(name = "Users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    /** Tên đăng nhập - duy nhất */
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập từ 3 đến 50 ký tự")
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /** Mật khẩu đã được mã hóa bằng BCrypt */
    @NotBlank(message = "Mật khẩu không được để trống")
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /** Họ và tên hiển thị */
    @Column(name = "full_name", length = 300)
    private String fullName;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    /** Số lần đăng nhập sai liên tiếp (dùng để Rate Limit / Lockout) */
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    /** Thời điểm hết hạn tạm khóa tài khoản (null = không bị khóa) */
    @Column(name = "lockout_until")
    private LocalDateTime lockoutUntil;

    @Column(name = "account_enabled", nullable = false, columnDefinition = "bit not null default 1")
    private boolean accountEnabled = true;

    @Column(name = "enabled", nullable = false, columnDefinition = "bit not null default 1")
    private boolean enabled = true;

    /** BCrypt hash of the password reset OTP */
    @Column(name = "reset_otp", length = 100)
    private String resetOtpHash;

    /** Expiration timestamp for the current reset OTP */
    @Column(name = "reset_otp_expiry")
    private LocalDateTime resetOtpExpiry;

    /** Number of failed OTP verification attempts for the current window */
    @Column(name = "reset_otp_failed_attempts")
    private Integer resetOtpFailedAttempts = 0;

    /** Number of OTP requests made in the current rate limit window */
    @Column(name = "reset_otp_request_count")
    private Integer resetOtpRequestCount = 0;

    /** Start of the current rate limit window for OTP requests */
    @Column(name = "reset_otp_request_window_start")
    private LocalDateTime resetOtpRequestWindowStart;

    /** Trạng thái tài khoản: APPROVED, PENDING_APPROVAL, REJECTED, ACTIVE, LOCKED */
    @Column(name = "account_status", nullable = false, length = 30, columnDefinition = "VARCHAR(30) DEFAULT 'APPROVED'")
    private String accountStatus = "APPROVED";

    @Column(name = "contact_address", length = 255)
    private String contactAddress;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "total_reviews")
    private Integer totalReviews = 0;

    @Column(name = "is_top_landlord")
    private boolean topLandlord = false;

    /** Vai trò người dùng (ADMIN, STAFF, USER, LANDLORD, TENANT) */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @OneToMany(mappedBy = "staff", fetch = FetchType.LAZY)
    private List<StaffBuildingAssignment> staffBuildingAssignments = new ArrayList<>();

    // ==================== Constructors ====================

    public User() {}

    public User(Integer userId, String username, String password, String fullName, Role role) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.failedLoginAttempts = 0;
        this.registeredAt = LocalDateTime.now();
    }

    public User(Integer userId, String username, String password, String fullName, String email, String phone, Role role) {
        this(userId, username, password, fullName, role);
        this.email = email;
        this.phone = phone;
    }

    // ==================== Business Methods ====================

    /** Kiểm tra xem tài khoản có đang bị tạm khóa (Rate limit) không */
    public boolean isAccountLocked() {
        return lockoutUntil != null && lockoutUntil.isAfter(LocalDateTime.now());
    }

    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(accountStatus);
    }

    public boolean isPendingApproval() {
        return "PENDING_APPROVAL".equalsIgnoreCase(accountStatus) || "PENDING".equalsIgnoreCase(accountStatus);
    }

    public boolean isRejected() {
        return "REJECTED".equalsIgnoreCase(accountStatus);
    }

    public boolean isActiveTenant() {
        return isEnabled() && (accountStatus == null || "APPROVED".equalsIgnoreCase(accountStatus) || "ACTIVE".equalsIgnoreCase(accountStatus));
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

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Integer getFailedLoginAttempts() { return failedLoginAttempts != null ? failedLoginAttempts : 0; }
    public void setFailedLoginAttempts(Integer failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public LocalDateTime getLockoutUntil() { return lockoutUntil; }
    public void setLockoutUntil(LocalDateTime lockoutUntil) { this.lockoutUntil = lockoutUntil; }

    public boolean isAccountEnabled() { return accountEnabled && enabled; }
    public void setAccountEnabled(boolean accountEnabled) {
        this.accountEnabled = accountEnabled;
        this.enabled = accountEnabled;
    }

    public boolean isEnabled() { return accountEnabled && enabled; }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.accountEnabled = enabled;
    }

    public String getResetOtpHash() { return resetOtpHash; }
    public void setResetOtpHash(String resetOtpHash) { this.resetOtpHash = resetOtpHash; }

    /** Delegate method for compatibility with incoming code */
    public String getResetOtp() { return resetOtpHash; }
    public void setResetOtp(String resetOtp) { this.resetOtpHash = resetOtp; }

    public LocalDateTime getResetOtpExpiry() { return resetOtpExpiry; }
    public void setResetOtpExpiry(LocalDateTime resetOtpExpiry) { this.resetOtpExpiry = resetOtpExpiry; }

    public Integer getResetOtpFailedAttempts() { return resetOtpFailedAttempts != null ? resetOtpFailedAttempts : 0; }
    public void setResetOtpFailedAttempts(Integer resetOtpFailedAttempts) { this.resetOtpFailedAttempts = resetOtpFailedAttempts; }

    public Integer getResetOtpRequestCount() { return resetOtpRequestCount != null ? resetOtpRequestCount : 0; }
    public void setResetOtpRequestCount(Integer resetOtpRequestCount) { this.resetOtpRequestCount = resetOtpRequestCount; }

    public LocalDateTime getResetOtpRequestWindowStart() { return resetOtpRequestWindowStart; }
    public void setResetOtpRequestWindowStart(LocalDateTime resetOtpRequestWindowStart) { this.resetOtpRequestWindowStart = resetOtpRequestWindowStart; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus != null ? accountStatus.toUpperCase() : "APPROVED";
    }

    public String getContactAddress() { return contactAddress; }
    public void setContactAddress(String contactAddress) { this.contactAddress = contactAddress; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(LocalDateTime rejectedAt) { this.rejectedAt = rejectedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    public List<StaffBuildingAssignment> getStaffBuildingAssignments() { return staffBuildingAssignments; }
    public void setStaffBuildingAssignments(List<StaffBuildingAssignment> staffBuildingAssignments) {
        this.staffBuildingAssignments = staffBuildingAssignments;
    }

    public BigDecimal getAverageRating() { return averageRating != null ? averageRating : BigDecimal.ZERO; }
    public void setAverageRating(BigDecimal averageRating) { this.averageRating = averageRating; }

    public Integer getTotalReviews() { return totalReviews != null ? totalReviews : 0; }
    public void setTotalReviews(Integer totalReviews) { this.totalReviews = totalReviews; }

    public boolean isTopLandlord() { return topLandlord; }
    public void setTopLandlord(boolean topLandlord) { this.topLandlord = topLandlord; }

    public boolean hasRating() {
        return averageRating != null && averageRating.compareTo(BigDecimal.ZERO) > 0;
    }

    public String getDisplayRating() {
        return hasRating() ? averageRating.setScale(1, java.math.RoundingMode.HALF_UP).toString() : "—";
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", role=" + (role != null ? role.getRoleName() : "null") +
                ", accountStatus='" + accountStatus + '\'' +
                ", accountEnabled=" + isAccountEnabled() +
                '}';
    }
}
