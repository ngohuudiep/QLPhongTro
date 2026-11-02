package com.qlphongtro.controller;

import com.qlphongtro.entity.RoomListing;
import com.qlphongtro.service.RoomListingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class HomeController {

    private final RoomListingService roomListingService;

    @Autowired
    public HomeController(RoomListingService roomListingService) {
        this.roomListingService = roomListingService;
    }

    @GetMapping("/")
    public String index(
            @RequestParam(name = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(name = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(name = "priceRange", required = false, defaultValue = "all") String priceRange,
            @RequestParam(name = "floorNo", required = false) Integer floorNo,
            @RequestParam(name = "sortBy", required = false, defaultValue = "price_asc") String sortBy,
            Model model) {

        List<RoomListing> rooms = roomListingService.filterRooms(keyword, status, priceRange, floorNo, sortBy);

        model.addAttribute("rooms", rooms);
        model.addAttribute("totalRooms", roomListingService.getAllRooms().size());
        model.addAttribute("totalAvailable", roomListingService.countAvailableRooms());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPrice", priceRange);
        model.addAttribute("selectedFloor", floorNo);
        model.addAttribute("selectedSort", sortBy);

        return "index";
    }

    @GetMapping("/rooms/list")
    public String roomList(
            @RequestParam(name = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(name = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(name = "priceRange", required = false, defaultValue = "all") String priceRange,
            @RequestParam(name = "floorNo", required = false) Integer floorNo,
            @RequestParam(name = "sortBy", required = false, defaultValue = "price_asc") String sortBy,
            Model model) {
        return index(keyword, status, priceRange, floorNo, sortBy, model);
    }

    @GetMapping("/rooms/{id}")
    public String roomDetail(@PathVariable("id") Integer id, Model model, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            RoomListing room = roomListingService.getById(id);
            model.addAttribute("room", room);
            // Gợi ý thêm 3 phòng tương tự
            List<RoomListing> similarRooms = roomListingService.getAvailableRooms().stream()
                    .filter(r -> !r.getListingId().equals(id))
                    .limit(3)
                    .toList();
            model.addAttribute("similarRooms", similarRooms);
            return "room/detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thông tin phòng ID: " + id);
            return "redirect:/rooms/list";
        }
    }
}
