package com.qlphongtro.service;

import com.qlphongtro.dto.ProfileDto;
import com.qlphongtro.dto.RegisterRequest;
import com.qlphongtro.dto.AdminUserForm;
import com.qlphongtro.entity.User;

import java.util.List;

public interface UserService {
    User registerUser(RegisterRequest request);
    User getByUsername(String username);
    void updateProfile(String username, ProfileDto.UpdateInfo dto);
    void changePassword(String username, ProfileDto.ChangePassword dto);
    void generateResetOtp(String identifier);
    boolean resetPasswordWithOtp(String identifier, String otp, String newPassword);
    List<User> getManagedUsers();
    User getManagedUser(Integer userId);
    User saveManagedUser(AdminUserForm form);
    void setAccountEnabled(Integer userId, boolean enabled);
}
