package com.qlphongtro.controller;

import com.qlphongtro.entity.RoomListing;
import com.qlphongtro.service.AdminWorkspaceService;
import com.qlphongtro.service.RoomListingService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoomController {

    private final RoomListingService roomListingService;
    private final AdminWorkspaceService adminWorkspaceService;
    private final com.qlphongtro.service.ReviewService reviewService;

    public AdminRoomController(RoomListingService roomListingService, AdminWorkspaceService adminWorkspaceService) {
        this(roomListingService, adminWorkspaceService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AdminRoomController(RoomListingService roomListingService,
                               AdminWorkspaceService adminWorkspaceService,
                               @org.springframework.beans.factory.annotation.Autowired(required = false) com.qlphongtro.service.ReviewService reviewService) {
        this.roomListingService = roomListingService;
        this.adminWorkspaceService = adminWorkspaceService;
        this.reviewService = reviewService;
    }

    @ModelAttribute("pendingCount")
    public long pendingCount() {
        return adminWorkspaceService != null ? adminWorkspaceService.countPendingLandlords() : 0L;
    }

    private Map<String, Long> safeStats() {
        Map<String, Long> stats = adminWorkspaceService != null ? adminWorkspaceService.getDashboardStats() : null;
        if (stats == null) {
            stats = new LinkedHashMap<>();
        } else {
            stats = new LinkedHashMap<>(stats);
        }
        for (String key : List.of("accountCount", "adminCount", "landlordCount", "tenantCount", "pendingLandlordCount",
                "approvedLandlordCount", "listingCount", "listingOccupiedCount", "listingAvailableCount",
                "listingMaintenanceCount", "managedRoomCount", "managedOccupiedCount", "managedAvailableCount",
                "managedMaintenanceCount", "buildingCount", "activeContractCount")) {
            stats.putIfAbsent(key, 0L);
        }
        return stats;
    }

    @GetMapping("/admin/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("stats", safeStats());
        model.addAttribute("reportedReviewsCount", reviewService != null ? reviewService.getReportedReviewsCount() : 0L);
        model.addAttribute("activePage", "dashboard");
        return "admin/dashboard";
    }

    @GetMapping("/admin/rooms")
    public String listAdminRooms(@RequestParam(name = "keyword", required = false) String keyword,
                                 @RequestParam(name = "status", required = false, defaultValue = "ALL") String status,
                                 @RequestParam(name = "ownerId", required = false) Integer ownerId,
                                 Model model) {
        String cleanKeyword = keyword == null ? "" : keyword.trim();
        String cleanStatus = status == null || status.isBlank() ? "ALL" : status.trim().toUpperCase();
        model.addAttribute("rooms", adminWorkspaceService != null ? adminWorkspaceService.getRoomListings(cleanKeyword, cleanStatus, ownerId) : List.of());
        model.addAttribute("owners", adminWorkspaceService != null ? adminWorkspaceService.getLandlords() : List.of());
        model.addAttribute("selectedOwnerId", ownerId);
        model.addAttribute("keyword", cleanKeyword);
        model.addAttribute("selectedStatus", cleanStatus);
        model.addAttribute("stats", safeStats());
        model.addAttribute("activePage", "rooms");
        return "admin/rooms";
    }

    @GetMapping("/admin/rooms/new")
    public String showCreateForm(Model model) {
        RoomListing room = new RoomListing();
        room.setStatus("TRONG");
        room.setFloorNo(1);
        room.setAmenities("Điều hòa, Wifi, Nóng lạnh, Gác lửng");
        model.addAttribute("room", room);
        prepareRoomForm(model, false);
        return "admin/room-form";
    }

    @GetMapping("/admin/rooms/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("room", roomListingService.getById(id));
            prepareRoomForm(model, true);
            return "admin/room-form";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/rooms";
        }
    }

    @GetMapping("/admin/rooms/{id}")
    public String showRoomDetails(@PathVariable Integer id, Model model) {
        model.addAttribute("room", roomListingService.getById(id));
        model.addAttribute("activePage", "rooms");
        return "admin/room-detail";
    }

    @PostMapping("/admin/rooms/save")
    public String saveRoom(@Valid @ModelAttribute("room") RoomListing room,
                           BindingResult bindingResult,
                           @RequestParam(required = false) Integer ownerId,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        boolean editing = room.getListingId() != null;
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", "Vui lòng kiểm tra lại các trường thông tin bị lỗi.");
            prepareRoomForm(model, editing);
            return "admin/room-form";
        }

        try {
            adminWorkspaceService.saveRoomListing(room, ownerId);
            redirectAttributes.addFlashAttribute("successMessage", editing
                    ? "Cập nhật bài đăng phòng thành công!" : "Thêm bài đăng phòng thành công!");
            return "redirect:/admin/rooms";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            prepareRoomForm(model, editing);
            return "admin/room-form";
        }
    }

    @PostMapping("/admin/rooms/delete/{id}")
    public String deleteRoom(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            adminWorkspaceService.deleteRoomListing(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa bài đăng phòng.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/rooms";
    }

    private void prepareRoomForm(Model model, boolean editing) {
        model.addAttribute("isEdit", editing);
        model.addAttribute("owners", adminWorkspaceService.getLandlords());
        model.addAttribute("activePage", "rooms");
    }
}
