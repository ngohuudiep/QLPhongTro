package com.qlphongtro.controller;

import com.qlphongtro.entity.Review;
import com.qlphongtro.entity.RoomListing;
import com.qlphongtro.entity.User;
import com.qlphongtro.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class RankingController {

    private final ReviewService reviewService;

    public RankingController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/ranking")
    public String rankingPage(Model model) {
        List<User> topLandlords = reviewService.getTopLandlords();
        List<RoomListing> topRooms = reviewService.getTopRatedRoomListings(10);

        model.addAttribute("topLandlords", topLandlords);
        model.addAttribute("topRooms", topRooms);

        return "public/ranking";
    }

    @GetMapping("/api/ranking/landlords")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> apiTopLandlords() {
        List<User> landlords = reviewService.getTopLandlords();
        List<Map<String, Object>> response = landlords.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", u.getUserId());
            map.put("fullName", u.getFullName());
            map.put("phone", u.getPhone());
            map.put("averageRating", u.getAverageRating());
            map.put("totalReviews", u.getTotalReviews());
            map.put("isTopLandlord", u.isTopLandlord());
            return map;
        }).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/ranking/rooms")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> apiTopRooms(@RequestParam(defaultValue = "10") int limit) {
        List<RoomListing> rooms = reviewService.getTopRatedRoomListings(limit);
        List<Map<String, Object>> response = rooms.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("listingId", r.getListingId());
            map.put("roomCode", r.getRoomCode());
            map.put("title", r.getTitle());
            map.put("price", r.getPrice());
            map.put("averageRating", r.getAverageRating());
            map.put("totalReviews", r.getTotalReviews());
            map.put("imageUrl", r.getImageUrl());
            return map;
        }).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/reviews/room/{roomId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> apiRoomReviews(@PathVariable Integer roomId) {
        List<Review> reviews = reviewService.getVisibleReviewsForRoom(roomId);
        List<Map<String, Object>> response = reviews.stream().map(rev -> {
            Map<String, Object> map = new HashMap<>();
            map.put("reviewId", rev.getReviewId());
            map.put("rating", rev.getRating());
            map.put("comment", rev.getComment());
            map.put("tenantName", rev.getTenantUser() != null ? rev.getTenantUser().getFullName() : "Khách thuê");
            map.put("createdAt", rev.getCreatedAt().toString());
            map.put("replyComment", rev.getReplyComment());
            map.put("repliedAt", rev.getRepliedAt() != null ? rev.getRepliedAt().toString() : null);
            return map;
        }).toList();
        return ResponseEntity.ok(response);
    }
}
