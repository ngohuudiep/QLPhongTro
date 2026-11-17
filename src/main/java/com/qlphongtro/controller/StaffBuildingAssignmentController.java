package com.qlphongtro.controller;

import com.qlphongtro.service.StaffBuildingAssignmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/staff-assignments")
@PreAuthorize("hasRole('ADMIN')")
public class StaffBuildingAssignmentController {
    private final StaffBuildingAssignmentService assignmentService;

    public StaffBuildingAssignmentController(StaffBuildingAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("assignments", assignmentService.getAllAssignments());
        model.addAttribute("staffUsers", assignmentService.getStaffUsers());
        model.addAttribute("buildings", assignmentService.getBuildings());
        return "admin/staff-assignments";
    }

    @PostMapping
    public String assign(@RequestParam Integer staffUserId, @RequestParam Integer buildingId,
                         RedirectAttributes redirectAttributes) {
        try {
            assignmentService.assign(staffUserId, buildingId);
            redirectAttributes.addFlashAttribute("successMessage", "Staff assignment saved.");
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin/staff-assignments";
    }

    @PostMapping("/delete")
    public String remove(@RequestParam Integer assignmentId, RedirectAttributes redirectAttributes) {
        assignmentService.remove(assignmentId);
        redirectAttributes.addFlashAttribute("successMessage", "Staff assignment removed.");
        return "redirect:/admin/staff-assignments";
    }
}
