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

/**
 * Controller quản lý phòng trọ dành riêng cho ADMIN.
 * Toàn bộ URL /admin/rooms/** chỉ ADMIN mới truy cập được.
 *
 * URL pattern:
 *   GET  /admin/rooms           → danh sách tất cả phòng
 *   GET  /admin/rooms/new       → form thêm mới
 *   GET  /admin/rooms/edit/{id} → form chỉnh sửa
 *   POST /admin/rooms/save      → lưu tạo mới / cập nhật
 *   POST /admin/rooms/delete/{id} → xóa phòng
 */
@Controller
@RequestMapping("/admin/rooms")
@PreAuthorize("hasRole('ADMIN')")   // Tất cả method đều yêu cầu ADMIN
public class AdminRoomController {

    private final RoomListingService roomListingService;

    @Autowired
    public AdminRoomController(RoomListingService roomListingService) {
        this.roomListingService = roomListingService;
    }

    // ======================================================
    // 1. Danh sách tất cả phòng (cả trống, đã thuê, bảo trì)
    // ======================================================
    @GetMapping
    public String listRooms(Model model) {
        model.addAttribute("rooms",       roomListingService.getAllRooms());
        model.addAttribute("totalTrong",   roomListingService.countAvailableRooms());
        model.addAttribute("totalDaTHue",  countByStatus("DA_THUE"));
        model.addAttribute("totalBaoTri",  countByStatus("BAO_TRI"));
        model.addAttribute("pageTitle",    "Quản lý Phòng trọ");
        return "admin/rooms";
    }

    // ======================================================
    // 2. Form thêm mới phòng (GET)
    // ======================================================
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        RoomListing room = new RoomListing();
        room.setStatus("TRONG");    // Mặc định trạng thái trống khi tạo mới
        model.addAttribute("room",      room);
        model.addAttribute("pageTitle", "Thêm phòng mới");
        model.addAttribute("isEdit",    false);
        return "admin/room-form";
    }

    // ======================================================
    // 3. Form chỉnh sửa phòng (GET)
    // ======================================================
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            RoomListing room = roomListingService.getById(id);
            model.addAttribute("room",      room);
            model.addAttribute("pageTitle", "Chỉnh sửa phòng: " + room.getRoomCode());
            model.addAttribute("isEdit",    true);
            return "admin/room-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Không tìm thấy phòng ID: " + id);
            return "redirect:/admin/rooms";
        }
    }

    // ======================================================
    // 4. Lưu phòng — Tạo mới hoặc Cập nhật (POST)
    //    Bao gồm logic cập nhật giá tiền từ form Admin.
    // ======================================================
    @PostMapping("/save")
    public String saveRoom(@Valid @ModelAttribute("room") RoomListing room,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        // Kiểm tra lỗi validation từ @NotBlank, @Size...
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", room.getListingId() == null ? "Thêm phòng mới" : "Chỉnh sửa phòng");
            model.addAttribute("isEdit",    room.getListingId() != null);
            model.addAttribute("errorMsg",  "Vui lòng kiểm tra lại các trường thông tin bên dưới.");
            return "admin/room-form";
        }

        try {
            boolean isNew = (room.getListingId() == null);
            // Service sẽ validate mã phòng trùng và giá tiền, rồi lưu vào SQL Server
            roomListingService.save(room);
            redirectAttributes.addFlashAttribute("successMsg",
                    isNew ? "✅ Thêm phòng mới thành công!" : "✅ Cập nhật phòng thành công!");
        } catch (IllegalArgumentException e) {
            // Lỗi nghiệp vụ (mã trùng, giá âm...)
            model.addAttribute("pageTitle", room.getListingId() == null ? "Thêm phòng mới" : "Chỉnh sửa phòng");
            model.addAttribute("isEdit",    room.getListingId() != null);
            model.addAttribute("errorMsg",  e.getMessage());
            return "admin/room-form";
        }

        return "redirect:/admin/rooms";
    }

    // ======================================================
    // 5. Xóa phòng (POST — tránh CSRF qua GET link)
    // ======================================================
    @PostMapping("/delete/{id}")
    public String deleteRoom(@PathVariable("id") Integer id,
                             RedirectAttributes redirectAttributes) {
        try {
            roomListingService.delete(id);
            redirectAttributes.addFlashAttribute("successMsg", "🗑️ Đã xóa phòng thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi khi xóa phòng: " + e.getMessage());
        }
        return "redirect:/admin/rooms";
    }

    // ======================================================
    // Helper — đếm phòng theo status để hiển thị stat card
    // ======================================================
    private long countByStatus(String status) {
        return roomListingService.getAllRooms().stream()
                .filter(r -> status.equals(r.getStatus()))
                .count();
    }
}
