package com.qlphongtro.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity ánh xạ bảng Roles trong SQL Server.
 * Lưu trữ các vai trò: ADMIN, STAFF, USER
 */
@Entity
@Table(name = "Roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer roleId;

    /** Tên vai trò: ADMIN, STAFF, USER */
    @Column(name = "role_name", nullable = false, unique = true, length = 20)
    private String roleName;

    /** Danh sách người dùng có vai trò này (quan hệ 1-Nhiều) */
    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    public Role() {}

    public Role(Integer roleId, String roleName) {
        this.roleId = roleId;
        this.roleName = roleName;
    }

    // ==================== Getters & Setters ====================

    public Integer getRoleId() { return roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }

    @Override
    public String toString() {
        return "Role{roleId=" + roleId + ", roleName='" + roleName + "'}";
    }
}
