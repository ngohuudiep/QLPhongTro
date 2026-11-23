package com.qlphongtro.controller;

import com.qlphongtro.dto.ReviewForm;
import com.qlphongtro.entity.Contract;
import com.qlphongtro.entity.Review;
import com.qlphongtro.entity.User;
import com.qlphongtro.repository.ReviewRepository;
import com.qlphongtro.repository.UserRepository;
import com.qlphongtro.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/tenant/reviews")
@PreAuthorize("hasAnyRole('TENANT', 'USER', 'NGUOITHUE', 'NGUOI_THUE')")
public class TenantReviewController {

    private final ReviewService reviewService;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public TenantReviewController(ReviewService reviewService,
                                  ReviewRepository reviewRepository,
                                  UserRepository userRepository) {
        this.reviewService = reviewService;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String reviewPage(Authentication authentication, Model model) {
        String username = authentication.getName();
        User tenant = userRepository.findByUsername(username).orElse(null);

        List<Contract> reviewableContracts = reviewService.getReviewableContractsForTenant(username);
        List<Review> myReviews = tenant != null
                ? reviewRepository.findByTenantUser_UserIdOrderByCreatedAtDesc(tenant.getUserId())
                : List.of();

        if (!model.containsAttribute("reviewForm")) {
            model.addAttribute("reviewForm", new ReviewForm());
        }
        model.addAttribute("reviewableContracts", reviewableContracts);
        model.addAttribute("myReviews", myReviews);

        return "tenant/reviews";
    }

    @PostMapping
    public String submitReview(@Valid @ModelAttribute("reviewForm") ReviewForm form,
                               BindingResult bindingResult,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            redirectAttributes.addFlashAttribute("reviewForm", form);
            return "redirect:/tenant/reviews";
        }

        try {
            reviewService.createReview(authentication.getName(), form.getContractId(), form.getRating(), form.getComment());
            redirectAttributes.addFlashAttribute("successMessage", "Cảm ơn bạn đã gửi đánh giá phòng thành công!");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("reviewForm", form);
        }

        return "redirect:/tenant/reviews";
    }
}
