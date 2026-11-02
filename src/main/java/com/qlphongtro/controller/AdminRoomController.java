package com.qlphongtro.controller;

import com.qlphongtro.entity.RoomListing;
import com.qlphongtro.service.RoomListingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

@Controller
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminRoomController {

    private final RoomListingService roomListingService;

    @Autowired
    public AdminRoomController(RoomListingService roomListingService) {
        this.roomListingService = roomListingService;
    }

    // ==============================================================
    // 1. DASHBOARD QUẢN TRỊ (GET /admin/dashboard)
    // ==============================================================
    @GetMapping("/admin/dashboard")
    public String showDashboard(Model model) {
        List<RoomListing> allRooms = roomListingService.getAllRooms();

        long totalRooms = allRooms.size();
        long totalTrong = allRooms.stream().filter(r -> "TRONG".equals(r.getStatus())).count();
        long totalDaThue = allRooms.stream().filter(r -> "DA_THUE".equals(r.getStatus())).count();
        long totalBaoTri = allRooms.stream().filter(r -> "BAO_TRI".equals(r.getStatus())).count();

        // Tỷ lệ lấp đầy (%)
        double occupancyRate = totalRooms > 0 ? ((double) totalDaThue / totalRooms) * 100.0 : 0.0;

        // Ước tính doanh thu tháng hiện tại từ các phòng ĐÃ THUÊ
        BigDecimal monthlyRevenue = allRooms.stream()
                .filter(r -> "DA_THUE".equals(r.getStatus()) && r.getPrice() != null)
                .map(RoomListing::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Doanh thu tối đa nếu lấp đầy 100%
        BigDecimal potentialRevenue = allRooms.stream()
                .filter(r -> r.getPrice() != null)
                .map(RoomListing::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DecimalFormat df = new DecimalFormat("###,###,###");

        model.addAttribute("totalRooms", totalRooms);
        model.addAttribute("totalTrong", totalTrong);
        model.addAttribute("totalDaThue", totalDaThue);
        model.addAttribute("totalBaoTri", totalBaoTri);
        model.addAttribute("occupancyRate", String.format("%.1f", occupancyRate));
        model.addAttribute("monthlyRevenue", df.format(monthlyRevenue) + " đ");
        model.addAttribute("potentialRevenue", df.format(potentialRevenue) + " đ");
        model.addAttribute("recentRooms", allRooms.stream().limit(6).toList());

        return "admin/dashboard";
    }

    // ==============================================================
    // 2. DANH SÁCH TẤT CẢ PHÒNG (GET /admin/rooms)
    // ==============================================================
    @GetMapping("/admin/rooms")
    public String listAdminRooms(
            @RequestParam(name = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(name = "status", required = false, defaultValue = "ALL") String status,
            Model model) {

        String cleanKeyword = (keyword != null) ? keyword.trim() : "";
        String cleanStatus = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : "ALL";

        List<RoomListing> allRooms = roomListingService.getAllRooms();
        long totalRooms = allRooms.size();
        long totalTrong = allRooms.stream().filter(r -> "TRONG".equalsIgnoreCase(r.getStatus())).count();
        long totalDaThue = allRooms.stream().filter(r -> "DA_THUE".equalsIgnoreCase(r.getStatus())).count();
        long totalBaoTri = allRooms.stream().filter(r -> "BAO_TRI".equalsIgnoreCase(r.getStatus())).count();

        List<RoomListing> rooms;
        if (!cleanKeyword.isEmpty() || !cleanStatus.equals("ALL")) {
            rooms = roomListingService.filterRooms(cleanKeyword, cleanStatus, "all", null, "newest");
        } else {
            rooms = allRooms;
        }

        model.addAttribute("rooms", rooms);
        model.addAttribute("keyword", cleanKeyword);
        model.addAttribute("selectedStatus", cleanStatus);
        model.addAttribute("totalRooms", totalRooms);
        model.addAttribute("totalTrong", totalTrong);
        model.addAttribute("totalDaThue", totalDaThue);
        model.addAttribute("totalBaoTri", totalBaoTri);

        return "admin/rooms";
    }

    // ==============================================================
    // 3. FORM THÊM MỚI PHÒNG (GET /admin/rooms/new)
    // ==============================================================
    @GetMapping("/admin/rooms/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String showCreateForm(Model model) {
        RoomListing room = new RoomListing();
        room.setStatus("TRONG");
        room.setFloorNo(1);
        room.setAmenities("Điều hòa, Wifi, Nóng lạnh, Gác lửng");
        model.addAttribute("room", room);
        model.addAttribute("isEdit", false);
        return "admin/room-form";
    }

    // ==============================================================
    // 4. FORM CHỈNH SỬA PHÒNG (GET /admin/rooms/edit/{id})
    // ==============================================================
    @GetMapping("/admin/rooms/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String showEditForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            RoomListing room = roomListingService.getById(id);
            model.addAttribute("room", room);
            model.addAttribute("isEdit", true);
            return "admin/room-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy phòng: " + e.getMessage());
            return "redirect:/admin/rooms";
        }
    }

    // ==============================================================
    // 5. LƯU TẠO MỚI HOẶC CẬP NHẬT (POST /admin/rooms/save)
    // ==============================================================
    @PostMapping("/admin/rooms/save")
    @PreAuthorize("hasRole('ADMIN')")
    public String saveRoom(
            @Valid @ModelAttribute("room") RoomListing room,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", room.getListingId() != null);
            model.addAttribute("errorMessage", "Vui lòng kiểm tra lại các trường thông tin bị lỗi.");
            return "admin/room-form";
        }

        try {
            boolean isNew = (room.getListingId() == null);
            roomListingService.save(room);
            redirectAttributes.addFlashAttribute("successMessage",
                    isNew ? "Thêm phòng mới thành công!" : "Cập nhật phòng thành công!");
            return "redirect:/admin/rooms";
        } catch (IllegalArgumentException e) {
            model.addAttribute("isEdit", room.getListingId() != null);
            model.addAttribute("errorMessage", e.getMessage());
            return "admin/room-form";
        }
    }

    // ==============================================================
    // 6. XÓA PHÒNG (POST /admin/rooms/delete/{id})
    // ==============================================================
    @PostMapping("/admin/rooms/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteRoom(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            roomListingService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa phòng thành công khỏi hệ thống!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa phòng: " + e.getMessage());
        }
        return "redirect:/admin/rooms";
    }

    private long countByStatus(String status) {
        return roomListingService.getAllRooms().stream()
                .filter(r -> status.equals(r.getStatus()))
                .count();
    }
}
