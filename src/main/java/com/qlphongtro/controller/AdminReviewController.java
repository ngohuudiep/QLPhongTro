package com.qlphongtro.controller;

import com.qlphongtro.entity.Review;
import com.qlphongtro.service.ReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ReviewService reviewService;

    public AdminReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public String reviewList(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(required = false, defaultValue = "ALL") String status,
                             Model model) {
        Page<Review> reviewPage = reviewService.getAllReviewsForAdmin(status, PageRequest.of(Math.max(0, page), 15));

        model.addAttribute("reviewPage", reviewPage);
        model.addAttribute("selectedStatus", status);

        return "admin/reviews";
    }

    @PostMapping("/{reviewId}/toggle")
    public String toggleVisibility(@PathVariable Integer reviewId,
                                   @RequestParam boolean visible,
                                   @RequestParam(defaultValue = "ALL") String status,
                                   @RequestParam(defaultValue = "0") int page,
                                   RedirectAttributes redirectAttributes) {
        try {
            reviewService.toggleReviewVisibility(reviewId, visible);
            redirectAttributes.addFlashAttribute("successMessage",
                    visible ? "Đã hiển thị lại đánh giá #" + reviewId : "Đã ẩn đánh giá #" + reviewId);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/admin/reviews?status=" + status + "&page=" + page;
    }

    @PostMapping("/{reviewId}/delete")
    public String deleteReview(@PathVariable Integer reviewId,
                               @RequestParam(defaultValue = "ALL") String status,
                               @RequestParam(defaultValue = "0") int page,
                               RedirectAttributes redirectAttributes) {
        try {
            reviewService.deleteReview(reviewId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa vĩnh viễn đánh giá #" + reviewId);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/admin/reviews?status=" + status + "&page=" + page;
    }
}
