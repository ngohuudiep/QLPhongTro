package com.qlphongtro.controller;

import com.qlphongtro.entity.RoomListing;
import com.qlphongtro.entity.BookingRequest;
import com.qlphongtro.service.RentalWorkflowService;
import com.qlphongtro.service.RoomListingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class HomeController {

    private final RoomListingService roomListingService;
    private final RentalWorkflowService rentalWorkflowService;
    private final com.qlphongtro.service.ReviewService reviewService;

    public HomeController(RoomListingService roomListingService, RentalWorkflowService rentalWorkflowService) {
        this(roomListingService, rentalWorkflowService, null);
    }

    @Autowired
    public HomeController(RoomListingService roomListingService,
                          RentalWorkflowService rentalWorkflowService,
                          @org.springframework.beans.factory.annotation.Autowired(required = false) com.qlphongtro.service.ReviewService reviewService) {
        this.roomListingService = roomListingService;
        this.rentalWorkflowService = rentalWorkflowService;
        this.reviewService = reviewService;
    }

    @GetMapping("/")
    public String index(
            @RequestParam(name = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(name = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(name = "priceRange", required = false, defaultValue = "all") String priceRange,
            @RequestParam(name = "floorNo", required = false) Integer floorNo,
            @RequestParam(name = "sortBy", required = false, defaultValue = "price_asc") String sortBy,
            Model model,
            Authentication authentication,
            HttpServletRequest request) {

        Object csrfAttribute = request.getAttribute(CsrfToken.class.getName());
        if (csrfAttribute instanceof CsrfToken csrfToken) {
            csrfToken.getToken();
        }
        List<RoomListing> rooms = roomListingService.filterRooms(keyword, status, priceRange, floorNo, sortBy);

        model.addAttribute("rooms", rooms);
        model.addAttribute("totalRooms", roomListingService.getAllRooms().size());
        model.addAttribute("totalAvailable", roomListingService.countAvailableRooms());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPrice", priceRange);
        model.addAttribute("selectedFloor", floorNo);
        model.addAttribute("selectedSort", sortBy);
        List<Integer> pendingRentalListingIds = new ArrayList<>();
        Map<Integer, String> rentalStatusByListingId = new LinkedHashMap<>();
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> List.of("ROLE_TENANT", "ROLE_NGUOITHUE", "ROLE_NGUOI_THUE")
                        .contains(authority.getAuthority()))) {
            for (BookingRequest rentalRequest : rentalWorkflowService.getTenantRequests(authentication.getName())) {
                String statusLabel = switch (rentalRequest.getStatus()) {
                    case "PENDING_LANDLORD_REVIEW" -> "Đang chờ chủ trọ xác nhận";
                    case "APPROVED_BY_LANDLORD" -> "Chủ trọ đã duyệt, chờ hợp đồng";
                    case "CONTRACT_SENT" -> "Hợp đồng đang chờ bạn xác nhận";
                    default -> null;
                };
                RoomListing room = rentalRequest.getRoom();
                if (statusLabel != null && room != null && room.getListingId() != null) {
                    pendingRentalListingIds.add(room.getListingId());
                    rentalStatusByListingId.put(room.getListingId(), statusLabel);
                }
            }
        }
        model.addAttribute("pendingRentalListingIds", pendingRentalListingIds);
        model.addAttribute("rentalStatusByListingId", rentalStatusByListingId);

        return "index";
    }

    @GetMapping("/rooms/list")
    public String roomList(
            @RequestParam(name = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(name = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(name = "priceRange", required = false, defaultValue = "all") String priceRange,
            @RequestParam(name = "floorNo", required = false) Integer floorNo,
            @RequestParam(name = "sortBy", required = false, defaultValue = "price_asc") String sortBy,
            Model model,
            Authentication authentication,
            HttpServletRequest request) {
        return index(keyword, status, priceRange, floorNo, sortBy, model, authentication, request);
    }

    @GetMapping("/rooms/{id}")
    public String roomDetail(@PathVariable("id") Integer id, Model model, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        RoomListing room;
        try {
            room = roomListingService.getById(id);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/rooms/list";
        }
        model.addAttribute("room", room);
        Integer physicalRoomId = room.getManagedRoom() != null ? room.getManagedRoom().getRoomId() : (room.getRoom() != null ? room.getRoom().getRoomId() : null);
        List<com.qlphongtro.entity.Review> reviews = (reviewService != null && physicalRoomId != null) ? reviewService.getVisibleReviewsForRoom(physicalRoomId) : List.of();
        model.addAttribute("reviews", reviews);

        List<RoomListing> similarRooms = roomListingService.getAvailableRooms().stream()
                .filter(r -> !r.getListingId().equals(id))
                .limit(3)
                .toList();
        model.addAttribute("similarRooms", similarRooms);
        return "room/detail";
    }

    @RequestMapping(value = "/access-denied", method = {RequestMethod.GET, RequestMethod.POST})
    public String accessDenied() {
        return "access-denied";
    }

}
