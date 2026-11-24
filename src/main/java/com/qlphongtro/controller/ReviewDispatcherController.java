package com.qlphongtro.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReviewDispatcherController {

    @GetMapping({"/reviews", "/danh-gia"})
    public String dispatchReviews(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (isAdmin) {
            return "redirect:/admin/reviews";
        }

        boolean isLandlord = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_LANDLORD".equals(a.getAuthority()) || "ROLE_STAFF".equals(a.getAuthority()));
        if (isLandlord) {
            return "redirect:/landlord/reviews";
        }

        return "redirect:/tenant/reviews";
    }
}
