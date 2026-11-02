package com.qlphongtro.service;

import com.qlphongtro.dto.ProfileDto;
import com.qlphongtro.dto.RegisterRequest;
import com.qlphongtro.entity.User;

public interface UserService {
    User registerUser(RegisterRequest request);
    User getByUsername(String username);
    void updateProfile(String username, ProfileDto.UpdateInfo dto);
    void changePassword(String username, ProfileDto.ChangePassword dto);
    String generateResetOtp(String identifier);
    void resetPasswordWithOtp(String identifier, String otp, String newPassword);
}
