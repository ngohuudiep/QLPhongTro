package com.qlphongtro.controller;

import com.qlphongtro.entity.Room;
import com.qlphongtro.service.BuildingService;
import com.qlphongtro.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService roomService;
    private final BuildingService buildingService;

    @Autowired
    public RoomController(RoomService roomService, BuildingService buildingService) {
        this.roomService = roomService;
        this.buildingService = buildingService;
    }

    // 1. Hiển thị danh sách phòng
    @GetMapping
    public String listRooms(Model model) {
        model.addAttribute("rooms", roomService.getAllRooms());
        return "room/list";
    }

    // 2. Hiển thị form thêm mới phòng
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("room", new Room());
        model.addAttribute("buildings", buildingService.getAllBuildings());
        return "room/form";
    }

    // 3. Hiển thị form chỉnh sửa phòng
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Room room = roomService.getRoomById(id);
            model.addAttribute("room", room);
            model.addAttribute("buildings", buildingService.getAllBuildings());
            return "room/form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/rooms";
        }
    }

    // 4. Lưu thông tin phòng (Thêm mới hoặc Cập nhật)
    @PostMapping("/save")
    public String saveRoom(@ModelAttribute("room") Room room, RedirectAttributes redirectAttributes) {
        try {
            roomService.saveRoom(room);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu thông tin phòng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu phòng: " + e.getMessage());
        }
        return "redirect:/rooms";
    }

    // 5. Xóa phòng
    @GetMapping("/delete/{id}")
    public String deleteRoom(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            roomService.deleteRoom(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa phòng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa phòng: " + e.getMessage());
        }
        return "redirect:/rooms";
    }
}
