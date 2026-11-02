package com.qlphongtro.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProfileDto {

    public static class UpdateInfo {
        @NotBlank(message = "Họ và tên không được để trống")
        @Size(max = 150)
        private String fullName;

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        private String email;

        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "^(0[3|5|7|8|9])[0-9]{8}$", message = "Số điện thoại Việt Nam không hợp lệ (10 chữ số)")
        private String phone;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }

    public static class ChangePassword {
        @NotBlank(message = "Mật khẩu hiện tại không được để trống")
        private String currentPassword;

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 8, message = "Mật khẩu mới tối thiểu 8 ký tự")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Mật khẩu phải chứa ít nhất một chữ cái và một chữ số")
        private String newPassword;

        @NotBlank(message = "Vui lòng nhập lại mật khẩu mới")
        private String confirmPassword;

        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }

    public static class ForgotPassword {
        @NotBlank(message = "Vui lòng nhập Email hoặc Số điện thoại")
        private String identifier;

        private String otp;

        private String newPassword;

        public String getIdentifier() { return identifier; }
        public void setIdentifier(String identifier) { this.identifier = identifier; }

        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }

        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}
