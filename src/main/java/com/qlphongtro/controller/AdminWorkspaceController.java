package com.qlphongtro.controller;

import com.qlphongtro.dto.AdminAccountForm;
import com.qlphongtro.entity.Building;
import com.qlphongtro.entity.Role;
import com.qlphongtro.entity.Room;
import com.qlphongtro.entity.User;
import com.qlphongtro.service.AdminWorkspaceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminWorkspaceController {

    private final AdminWorkspaceService adminWorkspaceService;

    public AdminWorkspaceController(AdminWorkspaceService adminWorkspaceService) {
        this.adminWorkspaceService = adminWorkspaceService;
    }

    @ModelAttribute("pendingCount")
    public long pendingCount() {
        return adminWorkspaceService.countPendingLandlords();
    }

    @GetMapping("/landlords/registrations")
    public String pendingLandlords(@RequestParam(required = false, defaultValue = "PENDING") String status,
                                   Model model) {
        String selectedStatus = status.trim().toUpperCase(Locale.ROOT);
        if (!List.of("ALL", "PENDING", "APPROVED", "REJECTED").contains(selectedStatus)) {
            selectedStatus = "PENDING";
        }
        model.addAttribute("pendingLandlords", adminWorkspaceService.getLandlordRegistrations(selectedStatus));
        model.addAttribute("selectedStatus", selectedStatus);
        model.addAttribute("approvedLandlordCount", adminWorkspaceService.countApprovedLandlords());
        model.addAttribute("activePage", "registrations");
        return "admin/registrations";
    }

    @GetMapping("/landlords/registrations/{id}")
    public String landlordDetails(@PathVariable Integer id, Model model) {
        model.addAttribute("account", adminWorkspaceService.getAccount(id, Role.LANDLORD));
        model.addAttribute("category", "landlords");
        model.addAttribute("categoryTitle", "Chủ trọ");
        model.addAttribute("activePage", "registrations");
        return "admin/landlord-registration-detail";
    }

    @PostMapping("/landlords/registrations/{id}/approve")
    public String approveLandlord(@PathVariable Integer id,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            adminWorkspaceService.reviewLandlord(id, true, null, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã duyệt tài khoản chủ trọ.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/landlords/registrations";
    }

    @PostMapping("/landlords/registrations/{id}/reject")
    public String rejectLandlord(@PathVariable Integer id,
                                 @RequestParam String reason,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            adminWorkspaceService.reviewLandlord(id, false, reason, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối yêu cầu đăng ký chủ trọ.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/landlords/registrations";
    }

    @GetMapping("/accounts/{category}")
    public String accounts(@PathVariable String category,
                           @RequestParam(required = false) String keyword,
                           Model model) {
        String role = toRole(category);
        model.addAttribute("accounts", adminWorkspaceService.getAccounts(role, keyword));
        model.addAttribute("category", category);
        model.addAttribute("categoryTitle", categoryTitle(role));
        model.addAttribute("keyword", keyword);
        model.addAttribute("activePage", role.equals(Role.LANDLORD) ? "landlords" : "tenants");
        return "admin/accounts";
    }

    @GetMapping("/accounts/{category}/new")
    public String newAccount(@PathVariable String category, Model model) {
        String role = toRole(category);
        model.addAttribute("accountForm", new AdminAccountForm());
        prepareAccountForm(model, category, role, false);
        return "admin/account-form";
    }

    @GetMapping("/accounts/{category}/{id}")
    public String accountDetails(@PathVariable String category, @PathVariable Integer id, Model model) {
        String role = toRole(category);
        model.addAttribute("account", adminWorkspaceService.getAccount(id, role));
        model.addAttribute("category", category);
        model.addAttribute("categoryTitle", categoryTitle(role));
        model.addAttribute("activePage", role.equals(Role.LANDLORD) ? "landlords" : "tenants");
        return "admin/account-detail";
    }

    @GetMapping("/accounts/{category}/{id}/edit")
    public String editAccount(@PathVariable String category, @PathVariable Integer id, Model model) {
        String role = toRole(category);
        User user = adminWorkspaceService.getAccount(id, role);
        AdminAccountForm form = new AdminAccountForm();
        form.setUserId(user.getUserId());
        form.setUsername(user.getUsername());
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());
        form.setPhone(user.getPhone());
        model.addAttribute("accountForm", form);
        prepareAccountForm(model, category, role, true);
        return "admin/account-form";
    }

    @PostMapping("/accounts/{category}/save")
    public String saveAccount(@PathVariable String category,
                              @ModelAttribute("accountForm") AdminAccountForm form,
                              RedirectAttributes redirectAttributes) {
        String role = toRole(category);
        try {
            adminWorkspaceService.saveAccount(form, role);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu tài khoản " + categoryTitle(role) + ".");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return form.getUserId() == null
                    ? "redirect:/admin/accounts/" + category + "/new"
                    : "redirect:/admin/accounts/" + category + "/" + form.getUserId() + "/edit";
        }
        return "redirect:/admin/accounts/" + category;
    }

    @PostMapping("/accounts/{category}/{id}/enabled")
    public String setEnabled(@PathVariable String category,
                             @PathVariable Integer id,
                             @RequestParam boolean enabled,
                             RedirectAttributes redirectAttributes) {
        String role = toRole(category);
        try {
            adminWorkspaceService.setAccountEnabled(id, role, enabled);
            redirectAttributes.addFlashAttribute("successMessage", enabled ? "Đã mở khóa tài khoản." : "Đã khóa tài khoản.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/accounts/" + category;
    }

    @PostMapping("/accounts/{category}/{id}/delete")
    public String deleteAccount(@PathVariable String category,
                                @PathVariable Integer id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        String role = toRole(category);
        try {
            adminWorkspaceService.deleteAccount(id, role, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa tài khoản.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/accounts/" + category;
    }

    @GetMapping("/buildings")
    public String buildings(Model model) {
        model.addAttribute("buildings", adminWorkspaceService.getManagedBuildings());
        model.addAttribute("activePage", "buildings");
        return "admin/buildings";
    }

    @GetMapping("/buildings/new")
    public String newBuilding(Model model) {
        model.addAttribute("building", new Building());
        model.addAttribute("owners", adminWorkspaceService.getLandlords());
        model.addAttribute("activePage", "buildings");
        return "admin/building-form";
    }

    @GetMapping("/buildings/{id}/edit")
    public String editBuilding(@PathVariable Integer id, Model model) {
        model.addAttribute("building", adminWorkspaceService.getManagedBuilding(id));
        model.addAttribute("owners", adminWorkspaceService.getLandlords());
        model.addAttribute("activePage", "buildings");
        return "admin/building-form";
    }

    @PostMapping("/buildings/save")
    public String saveBuilding(@ModelAttribute Building building,
                               @RequestParam(required = false) Integer ownerId,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            adminWorkspaceService.saveManagedBuilding(building, ownerId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu khu trọ.");
            return "redirect:/admin/buildings";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("building", building);
            model.addAttribute("owners", adminWorkspaceService.getLandlords());
            model.addAttribute("activePage", "buildings");
            return "admin/building-form";
        }
    }

    @PostMapping("/buildings/{id}/delete")
    public String deleteBuilding(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            adminWorkspaceService.deleteManagedBuilding(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa khu trọ.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/buildings";
    }

    @GetMapping("/managed-rooms")
    public String managedRooms(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false, defaultValue = "ALL") String status,
                               @RequestParam(required = false) Integer ownerId,
                               Model model) {
        model.addAttribute("rooms", adminWorkspaceService.getManagedRooms(keyword, status, ownerId));
        model.addAttribute("buildings", adminWorkspaceService.getManagedBuildings());
        model.addAttribute("owners", adminWorkspaceService.getLandlords());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedOwnerId", ownerId);
        model.addAttribute("activePage", "managed-rooms");
        return "admin/managed-rooms";
    }

    @GetMapping("/managed-rooms/new")
    public String newManagedRoom(Model model) {
        model.addAttribute("room", new Room());
        model.addAttribute("buildings", adminWorkspaceService.getManagedBuildings());
        model.addAttribute("activePage", "managed-rooms");
        return "admin/managed-room-form";
    }

    @GetMapping("/managed-rooms/{id}")
    public String managedRoomDetails(@PathVariable Integer id, Model model) {
        model.addAttribute("room", adminWorkspaceService.getManagedRoom(id));
        model.addAttribute("activePage", "managed-rooms");
        return "admin/managed-room-detail";
    }

    @GetMapping("/managed-rooms/{id}/edit")
    public String editManagedRoom(@PathVariable Integer id, Model model) {
        model.addAttribute("room", adminWorkspaceService.getManagedRoom(id));
        model.addAttribute("buildings", adminWorkspaceService.getManagedBuildings());
        model.addAttribute("activePage", "managed-rooms");
        return "admin/managed-room-form";
    }

    @PostMapping("/managed-rooms/save")
    public String saveManagedRoom(@ModelAttribute Room room,
                                  @RequestParam Integer buildingId,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        try {
            adminWorkspaceService.saveManagedRoom(room, buildingId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu phòng.");
            return "redirect:/admin/managed-rooms";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("room", room);
            model.addAttribute("buildings", adminWorkspaceService.getManagedBuildings());
            model.addAttribute("activePage", "managed-rooms");
            return "admin/managed-room-form";
        }
    }

    @PostMapping("/managed-rooms/{id}/delete")
    public String deleteManagedRoom(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            adminWorkspaceService.deleteManagedRoom(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa phòng.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/managed-rooms";
    }

    private static void prepareAccountForm(Model model, String category, String role, boolean editing) {
        model.addAttribute("category", category);
        model.addAttribute("categoryTitle", categoryTitle(role));
        model.addAttribute("editing", editing);
        model.addAttribute("activePage", role.equals(Role.LANDLORD) ? "landlords" : "tenants");
    }

    private static String toRole(String category) {
        if ("landlords".equalsIgnoreCase(category)) return Role.LANDLORD;
        if ("tenants".equalsIgnoreCase(category)) return Role.TENANT;
        throw new IllegalArgumentException("Danh mục tài khoản không hợp lệ.");
    }

    private static String categoryTitle(String role) {
        return Role.LANDLORD.equals(role) ? "Chủ trọ" : "Người thuê";
    }
}
