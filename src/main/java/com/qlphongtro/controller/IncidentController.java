package com.qlphongtro.controller;

import com.qlphongtro.entity.Incident;
import com.qlphongtro.service.IncidentService;
import com.qlphongtro.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final RoomService roomService;

    @Autowired
    public IncidentController(IncidentService incidentService, RoomService roomService) {
        this.incidentService = incidentService;
        this.roomService = roomService;
    }

    // 1. Danh sách báo cáo sự cố
    @GetMapping
    public String listIncidents(Model model) {
        model.addAttribute("incidents", incidentService.getAllIncidents());
        return "incident/list";
    }

    // 2. Form báo cáo sự cố mới
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        Incident incident = new Incident();
        incident.setPriority("Medium");
        incident.setStatus("Tiếp nhận");
        incident.setReportedAt(LocalDateTime.now());

        model.addAttribute("incident", incident);
        model.addAttribute("rooms", roomService.getAllRooms());
        return "incident/form";
    }

    // 3. Form chỉnh sửa/cập nhật xử lý sự cố
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Incident incident = incidentService.getIncidentById(id);
            model.addAttribute("incident", incident);
            model.addAttribute("rooms", roomService.getAllRooms());
            return "incident/form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/incidents";
        }
    }

    // 4. Lưu sự cố
    @PostMapping("/save")
    public String saveIncident(@ModelAttribute("incident") Incident incident, RedirectAttributes redirectAttributes) {
        try {
            incidentService.saveIncident(incident);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu thông tin sự cố thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/incidents";
    }

    // 5. Xóa sự cố
    @GetMapping("/delete/{id}")
    public String deleteIncident(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            incidentService.deleteIncident(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa sự cố thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/incidents";
    }
}
