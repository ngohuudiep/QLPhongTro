package com.qlphongtro.controller;

import com.qlphongtro.entity.Building;
import com.qlphongtro.service.BuildingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/buildings")
public class BuildingController {

    private final BuildingService buildingService;

    @Autowired
    public BuildingController(BuildingService buildingService) {
        this.buildingService = buildingService;
    }

    // 1. Hiển thị danh sách tòa nhà
    @GetMapping
    public String listBuildings(Model model) {
        model.addAttribute("buildings", buildingService.getAllBuildings());
        return "building/list";
    }

    // 2. Hiển thị form thêm mới tòa nhà
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("building", new Building());
        return "building/form";
    }

    // 3. Hiển thị form chỉnh sửa tòa nhà
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Building building = buildingService.getBuildingById(id);
            model.addAttribute("building", building);
            return "building/form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/buildings";
        }
    }

    // 4. Lưu tòa nhà (Dùng chung cho cả thêm mới và cập nhật)
    @PostMapping("/save")
    public String saveBuilding(@ModelAttribute("building") Building building, RedirectAttributes redirectAttributes) {
        try {
            buildingService.saveBuilding(building);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu thông tin tòa nhà thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/buildings";
    }

    // 5. Xóa tòa nhà theo ID
    @GetMapping("/delete/{id}")
    public String deleteBuilding(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            buildingService.deleteBuilding(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa tòa nhà thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/buildings";
    }
}
