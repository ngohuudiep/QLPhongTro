package com.qlphongtro.controller;

import com.qlphongtro.entity.Tenant;
import com.qlphongtro.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/tenants")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class TenantController {

    private final TenantService tenantService;

    @Autowired
    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    // 1. Hiển thị danh sách khách thuê (có hỗ trợ tìm kiếm)
    @GetMapping
    public String listTenants(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            model.addAttribute("tenants", tenantService.searchTenants(keyword));
            model.addAttribute("keyword", keyword);
        } else {
            model.addAttribute("tenants", tenantService.getAllTenants());
        }
        return "tenant/list";
    }

    // 2. Hiển thị form thêm mới khách thuê
    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String showCreateForm(Model model) {
        model.addAttribute("tenant", new Tenant());
        return "tenant/form";
    }

    // 3. Hiển thị form chỉnh sửa khách thuê
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Tenant tenant = tenantService.getTenantById(id);
            model.addAttribute("tenant", tenant);
            model.addAttribute("unlinkedUsers", tenantService.getUnlinkedUserAccounts());
            return "tenant/form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/tenants";
        }
    }

    // 4. Lưu khách thuê (Hỗ trợ @Valid kiểm tra lỗi hợp lệ)
    @PostMapping("/save")
    @PreAuthorize("hasRole('ADMIN') or #tenant.tenantId != null")
    public String saveTenant(
            @Valid @ModelAttribute("tenant") Tenant tenant,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Kiểm tra trùng lặp số CCCD khi thêm mới
        if (tenant.getTenantId() == null && tenantService.existsByIdNumber(tenant.getIdNumber())) {
            bindingResult.rejectValue("idNumber", "error.tenant", "Số CCCD/CMND này đã tồn tại trong hệ thống!");
        }

        // Nếu có lỗi validate, trả về lại form và hiển thị thông báo lỗi
        if (bindingResult.hasErrors()) {
            model.addAttribute("unlinkedUsers", tenantService.getUnlinkedUserAccounts());
            return "tenant/form";
        }

        try {
            tenantService.saveTenant(tenant);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu thông tin khách thuê thành công!");
        } catch (Exception e) {
            if (e instanceof AccessDeniedException) {
                throw e;
            }
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi lưu dữ liệu: " + e.getMessage());
        }
        return "redirect:/tenants";
    }

    @PostMapping("/{tenantId}/link-user")
    public String linkUserAccount(@PathVariable Integer tenantId,
            @RequestParam Integer userId, RedirectAttributes redirectAttributes) {
        tenantService.linkUserAccount(tenantId, userId);
        redirectAttributes.addFlashAttribute("successMessage", "Đã liên kết tài khoản người thuê.");
        return "redirect:/tenants/edit/" + tenantId;
    }

    // 5. Xóa khách thuê
    @PostMapping("/delete/{id}")
    public String deleteTenant(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            tenantService.deleteTenant(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa khách thuê thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/tenants";
    }
}
