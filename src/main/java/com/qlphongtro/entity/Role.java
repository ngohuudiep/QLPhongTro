package com.qlphongtro.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Entity ánh xạ bảng Roles trong SQL Server.
 * Lưu trữ các vai trò: ADMIN, STAFF, USER, LANDLORD, TENANT.
 */
@Entity
@Table(name = "Roles")
public class Role {

    public static final String ADMIN = "ADMIN";
    public static final String LANDLORD = "LANDLORD";
    public static final String TENANT = "TENANT";
    public static final String STAFF = "STAFF";
    public static final String USER = "USER";
    public static final String LEGACY_STAFF = "STAFF";
    public static final String LEGACY_USER = "USER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer roleId;

    /** Tên vai trò chính thức */
    @Column(name = "role_name", nullable = false, unique = true, length = 20)
    private String roleName;

    /** Danh sách người dùng có vai trò này (quan hệ 1-Nhiều) */
    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    public Role() {}

    public Role(Integer roleId, String roleName) {
        this.roleId = roleId;
        this.roleName = roleName != null ? roleName.trim().toUpperCase(Locale.ROOT) : null;
    }

    public static String normalizeRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return "";
        }

        String normalized = roleName.trim().toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        return switch (normalized) {
            case ADMIN, "QUAN_TRI", "QUAN_TRI_VIEN", "QUẢN_TRỊ", "QUẢN_TRỊ_VIÊN" -> ADMIN;
            case LANDLORD, "CHUTRO", "CHU_TRO", "CHỦ_TRỌ", LEGACY_STAFF -> LANDLORD;
            case TENANT, "NGUOITHUE", "NGUOI_THUE", "NGƯỜI_THUÊ", LEGACY_USER -> TENANT;
            default -> normalized;
        };
    }

    public Integer getRoleId() { return roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) {
        this.roleName = roleName != null ? roleName.trim().toUpperCase(Locale.ROOT) : null;
    }

    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }

    public boolean isAdmin() { return ADMIN.equalsIgnoreCase(normalizeRoleName(roleName)); }
    public boolean isLandlord() { return LANDLORD.equalsIgnoreCase(normalizeRoleName(roleName)) || STAFF.equalsIgnoreCase(roleName); }
    public boolean isTenant() { return TENANT.equalsIgnoreCase(normalizeRoleName(roleName)) || USER.equalsIgnoreCase(roleName); }

    @Override
    public String toString() {
        return "Role{" + "roleId=" + roleId + ", roleName='" + roleName + "'}";
    }
}
