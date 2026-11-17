package com.qlphongtro.controller;

import com.qlphongtro.dto.AdminUserForm;
import com.qlphongtro.entity.User;
import com.qlphongtro.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.getManagedUsers());
        return "admin/users";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new AdminUserForm());
        return "admin/user-form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        User user = userService.getManagedUser(id);
        AdminUserForm form = new AdminUserForm();
        form.setUserId(user.getUserId());
        form.setUsername(user.getUsername());
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());
        form.setPhone(user.getPhone());
        form.setRoleName(user.getRole().getRoleName());
        model.addAttribute("form", form);
        return "admin/user-form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("form") AdminUserForm form,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/user-form";
        }
        try {
            userService.saveManagedUser(form);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu tài khoản.");
            return "redirect:/admin/users";
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/user-form";
        }
    }

    @PostMapping("/{id}/enabled")
    public String setEnabled(@PathVariable Integer id, @RequestParam boolean enabled,
            RedirectAttributes redirectAttributes) {
        userService.setAccountEnabled(id, enabled);
        redirectAttributes.addFlashAttribute("successMessage", enabled ? "Đã mở khóa tài khoản." : "Đã khóa tài khoản.");
        return "redirect:/admin/users";
    }
}
