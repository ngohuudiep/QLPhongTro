package com.qlphongtro.controller;

import com.qlphongtro.entity.RoomListing;
import com.qlphongtro.service.RoomListingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller trang chủ khách hàng (User View).
 * Xử lý danh sách phòng trống, bộ lọc giá và tìm kiếm.
 *
 * Phân quyền (khai báo trong SecurityConfig):
 *   - GET /           → permitAll (không cần đăng nhập)
 *   - GET /rooms/list → permitAll
 */
@Controller
public class HomeController {

    private final RoomListingService roomListingService;

    @Autowired
    public HomeController(RoomListingService roomListingService) {
        this.roomListingService = roomListingService;
    }

    /**
     * Trang chủ - hiển thị danh sách phòng trống.
     * Hỗ trợ lọc theo khoảng giá và tìm kiếm từ khóa.
     *
     * @param priceRange  khoảng giá: "under2m", "2to4m", "over4m", hoặc null (tất cả)
     * @param keyword     từ khóa tìm kiếm (tên phòng / mô tả)
     * @param model       Thymeleaf model
     */
    @GetMapping("/")
    public String index(
            @RequestParam(name = "priceRange", required = false, defaultValue = "all") String priceRange,
            @RequestParam(name = "keyword",    required = false, defaultValue = "")   String keyword,
            Model model) {

        List<RoomListing> rooms;

        // Ưu tiên tìm kiếm nếu có keyword
        if (!keyword.trim().isEmpty()) {
            rooms = roomListingService.searchRooms(keyword);
        } else {
            // Lọc theo khoảng giá
            rooms = switch (priceRange) {
                case "under2m" -> roomListingService.getAvailableRoomsByPriceRange(
                        BigDecimal.ZERO, new BigDecimal("2000000"));
                case "2to4m"   -> roomListingService.getAvailableRoomsByPriceRange(
                        new BigDecimal("2000000"), new BigDecimal("4000000"));
                case "over4m"  -> roomListingService.getAvailableRoomsByPriceRange(
                        new BigDecimal("4000000"), new BigDecimal("999999999"));
                default        -> roomListingService.getAvailableRooms();  // "all"
            };
        }

        model.addAttribute("rooms",           rooms);
        model.addAttribute("totalAvailable",  roomListingService.countAvailableRooms());
        model.addAttribute("selectedFilter",  priceRange);
        model.addAttribute("keyword",         keyword);

        return "index";
    }

    /**
     * URL alias - /rooms/list cũng dẫn đến trang chủ danh sách phòng.
     * Được khai báo là permitAll trong SecurityConfig.
     */
    @GetMapping("/rooms/list")
    public String roomList(
            @RequestParam(name = "priceRange", required = false, defaultValue = "all") String priceRange,
            @RequestParam(name = "keyword",    required = false, defaultValue = "")   String keyword,
            Model model) {
        return index(priceRange, keyword, model);
    }
}
