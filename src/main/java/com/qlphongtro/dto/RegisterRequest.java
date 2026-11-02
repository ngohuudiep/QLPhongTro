package com.qlphongtro.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO nhận dữ liệu từ Form Đăng ký tài khoản.
 * Tích hợp validate phía Server đầy đủ:
 *  - Email đúng định dạng
 *  - SĐT Việt Nam hợp lệ (đầu 03, 05, 07, 08, 09 gồm 10 số)
 *  - Mật khẩu tối thiểu 8 ký tự, có cả chữ và số
 */
public class RegisterRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
    private String fullName;

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 4, max = 50, message = "Tên đăng nhập từ 4 đến 50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9_.]+$", message = "Tên đăng nhập chỉ chứa chữ cái, chữ số, dấu gạch dưới hoặc chấm")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0[3|5|7|8|9])[0-9]{8}$", message = "Số điện thoại Việt Nam không hợp lệ (10 chữ số)")
    private String phone;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Mật khẩu phải chứa ít nhất một chữ cái và một chữ số")
    private String password;

    @NotBlank(message = "Vui lòng nhập lại mật khẩu xác nhận")
    private String confirmPassword;

    /** Vai trò lựa chọn: "ADMIN" (Chủ trọ) hoặc "USER" (Người thuê) */
    @NotBlank(message = "Vui lòng chọn vai trò của bạn")
    private String roleChoice = "USER";

    public RegisterRequest() {}

    // Getters and Setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public String getRoleChoice() { return roleChoice; }
    public void setRoleChoice(String roleChoice) { this.roleChoice = roleChoice; }
}
