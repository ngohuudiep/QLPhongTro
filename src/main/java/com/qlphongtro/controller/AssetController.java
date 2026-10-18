package com.qlphongtro.controller;

import com.qlphongtro.entity.Asset;
import com.qlphongtro.service.AssetService;
import com.qlphongtro.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/assets")
public class AssetController {

    private final AssetService assetService;
    private final RoomService roomService;

    @Autowired
    public AssetController(AssetService assetService, RoomService roomService) {
        this.assetService = assetService;
        this.roomService = roomService;
    }

    // 1. Danh sách tài sản trong các phòng
    @GetMapping
    public String listAssets(Model model) {
        model.addAttribute("assets", assetService.getAllAssets());
        return "asset/list";
    }

    // 2. Form thêm tài sản
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        Asset asset = new Asset();
        asset.setQuantity((short) 1);
        asset.setConditionStatus("Bình thường");

        model.addAttribute("asset", asset);
        model.addAttribute("rooms", roomService.getAllRooms());
        return "asset/form";
    }

    // 3. Form chỉnh sửa tài sản
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Asset asset = assetService.getAssetById(id);
            model.addAttribute("asset", asset);
            model.addAttribute("rooms", roomService.getAllRooms());
            return "asset/form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/assets";
        }
    }

    // 4. Lưu tài sản
    @PostMapping("/save")
    public String saveAsset(@ModelAttribute("asset") Asset asset, RedirectAttributes redirectAttributes) {
        try {
            assetService.saveAsset(asset);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu thông tin tài sản thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/assets";
    }

    // 5. Xóa tài sản
    @GetMapping("/delete/{id}")
    public String deleteAsset(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            assetService.deleteAsset(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa tài sản thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa: " + e.getMessage());
        }
        return "redirect:/assets";
    }
}
