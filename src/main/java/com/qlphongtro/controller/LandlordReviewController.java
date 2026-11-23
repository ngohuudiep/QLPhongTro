package com.qlphongtro.controller;

import com.qlphongtro.dto.ReviewReplyForm;
import com.qlphongtro.entity.Review;
import com.qlphongtro.entity.User;
import com.qlphongtro.repository.UserRepository;
import com.qlphongtro.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/landlord/reviews")
@PreAuthorize("hasAnyRole('LANDLORD', 'STAFF')")
public class LandlordReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    public LandlordReviewController(ReviewService reviewService, UserRepository userRepository) {
        this.reviewService = reviewService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String reviewList(@RequestParam(defaultValue = "0") int page,
                             Authentication authentication,
                             Model model) {
        String username = authentication.getName();
        User landlord = userRepository.findByUsername(username).orElse(null);

        Page<Review> reviewPage = reviewService.getLandlordReviews(username, PageRequest.of(Math.max(0, page), 10));

        model.addAttribute("landlord", landlord);
        model.addAttribute("reviewPage", reviewPage);
        model.addAttribute("replyForm", new ReviewReplyForm());

        return "landlord/reviews";
    }

    @PostMapping("/{reviewId}/reply")
    public String replyReview(@PathVariable Integer reviewId,
                              @Valid @ModelAttribute("replyForm") ReviewReplyForm form,
                              BindingResult bindingResult,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/landlord/reviews";
        }

        try {
            reviewService.replyReview(authentication.getName(), reviewId, form.getReplyComment());
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi phản hồi đánh giá thành công.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/landlord/reviews";
    }
}
