package com.qlphongtro.controller;

import com.qlphongtro.dto.ProfileDto;
import com.qlphongtro.dto.RegisterRequest;
import com.qlphongtro.entity.User;
import com.qlphongtro.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // ==============================================================
    // 1. Trang Đăng nhập (GET)
    // ==============================================================
    @GetMapping("/login")
    public String showLoginForm(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "registered", required = false) String registered,
            Model model) {

        if (error != null) {
            model.addAttribute("errorMessage", "Thông tin tài khoản hoặc mật khẩu không chính xác.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "Bạn đã đăng xuất khỏi hệ thống thành công.");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Đăng ký tài khoản thành công! Vui lòng đăng nhập.");
        }

        return "auth/login";
    }

    // ==============================================================
    // 2. Trang Đăng ký (GET)
    // ==============================================================
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    // ==============================================================
    // 3. Xử lý Đăng ký tài khoản (POST)
    // ==============================================================
    @PostMapping("/register")
    public String handleRegister(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Validate lỗi form (email, SĐT, độ dài password)
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerUser(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Hãy đăng nhập ngay.");
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        }
    }

    // ==============================================================
    // 4. Trang Quên mật khẩu & Đặt lại qua OTP (GET & POST)
    // ==============================================================
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        model.addAttribute("step", 1);
        model.addAttribute("forgotDto", new ProfileDto.ForgotPassword());
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password/send-otp")
    public String sendOtp(
            @ModelAttribute("forgotDto") ProfileDto.ForgotPassword dto,
            Model model) {
        try {
            String otp = userService.generateResetOtp(dto.getIdentifier());
            model.addAttribute("step", 2);
            model.addAttribute("demoOtp", otp); // Hiển thị mã demo trên màn hình để kiểm tra nhanh
            model.addAttribute("forgotDto", dto);
            model.addAttribute("successMessage", "Mã xác thực OTP đã được tạo (Demo: " + otp + "). Mã có hiệu lực trong 10 phút.");
        } catch (Exception e) {
            model.addAttribute("step", 1);
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password/reset")
    public String resetPassword(
            @ModelAttribute("forgotDto") ProfileDto.ForgotPassword dto,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            userService.resetPasswordWithOtp(dto.getIdentifier(), dto.getOtp(), dto.getNewPassword());
            redirectAttributes.addFlashAttribute("successMessage", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập bằng mật khẩu mới.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("step", 2);
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/forgot-password";
        }
    }

    // ==============================================================
    // 5. Hồ sơ cá nhân & Đổi mật khẩu (GET & POST)
    // ==============================================================
    @GetMapping("/profile")
    public String showProfile(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        User user = userService.getByUsername(authentication.getName());
        model.addAttribute("user", user);

        ProfileDto.UpdateInfo updateDto = new ProfileDto.UpdateInfo();
        updateDto.setFullName(user.getFullName());
        updateDto.setEmail(user.getEmail());
        updateDto.setPhone(user.getPhone());
        model.addAttribute("updateDto", updateDto);

        model.addAttribute("changePassDto", new ProfileDto.ChangePassword());
        return "auth/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @Valid @ModelAttribute("updateDto") ProfileDto.UpdateInfo dto,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (authentication == null) return "redirect:/login";

        if (bindingResult.hasErrors()) {
            User user = userService.getByUsername(authentication.getName());
            model.addAttribute("user", user);
            model.addAttribute("changePassDto", new ProfileDto.ChangePassword());
            model.addAttribute("errorMessage", "Vui lòng kiểm tra lại thông tin cập nhật.");
            return "auth/profile";
        }

        try {
            userService.updateProfile(authentication.getName(), dto);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin cá nhân thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(
            @Valid @ModelAttribute("changePassDto") ProfileDto.ChangePassword dto,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (authentication == null) return "redirect:/login";

        if (bindingResult.hasErrors()) {
            User user = userService.getByUsername(authentication.getName());
            model.addAttribute("user", user);
            ProfileDto.UpdateInfo updateDto = new ProfileDto.UpdateInfo();
            updateDto.setFullName(user.getFullName());
            updateDto.setEmail(user.getEmail());
            updateDto.setPhone(user.getPhone());
            model.addAttribute("updateDto", updateDto);
            model.addAttribute("errorMessage", "Vui lòng nhập mật khẩu mới đúng yêu cầu (tối thiểu 8 ký tự, gồm cả chữ và số).");
            return "auth/profile";
        }

        try {
            userService.changePassword(authentication.getName(), dto);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/profile";
    }
}
