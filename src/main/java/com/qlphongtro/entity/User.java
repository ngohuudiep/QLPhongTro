package com.qlphongtro.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Entity ánh xạ bảng Users trong SQL Server.
 * Lưu tài khoản đăng nhập của người dùng hệ thống.
 * Quan hệ N-1 với Role (nhiều User cùng một Role).
 */
@Entity
@Table(name = "Users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    /** Tên đăng nhập - phải là duy nhất trong hệ thống */
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(max = 50, message = "Tên đăng nhập tối đa 50 ký tự")
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /** Mật khẩu đã được mã hóa BCrypt trước khi lưu vào DB */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /** Họ và tên hiển thị của người dùng */
    @Size(max = 150, message = "Họ và tên tối đa 150 ký tự")
    @Column(name = "full_name", length = 150)
    private String fullName;

    /**
     * Vai trò của người dùng - quan hệ N-1 với bảng Roles.
     * Nhiều người dùng có thể có cùng một role.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    public User() {}

    public User(Integer userId, String username, String password, String fullName, Role role) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
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

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    @Override
    public String toString() {
        return "User{userId=" + userId + ", username='" + username + "', fullName='" + fullName + "'}";
    }
}
