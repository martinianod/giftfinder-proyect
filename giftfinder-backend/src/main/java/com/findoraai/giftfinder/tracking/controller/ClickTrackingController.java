package com.findoraai.giftfinder.tracking.controller;

import com.findoraai.giftfinder.tracking.dto.ClickAnalyticsResponse;
import com.findoraai.giftfinder.tracking.dto.ClickResponse;
import com.findoraai.giftfinder.tracking.dto.CreateClickRequest;
import com.findoraai.giftfinder.tracking.service.ClickTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ClickTrackingController {

    private final ClickTrackingService clickTrackingService;

    @PostMapping("/clicks")
    public ResponseEntity<ClickResponse> createClick(@Valid @RequestBody CreateClickRequest request) {
        Long userId = getCurrentUserId();
        ClickResponse response = clickTrackingService.createClick(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/r/{clickId}")
    public ResponseEntity<Void> redirect(@PathVariable String clickId) {
        String targetUrl = clickTrackingService.getRedirectUrl(clickId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(targetUrl))
                .build();
    }

    @GetMapping("/clicks/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClickAnalyticsResponse> getAnalytics(
            @RequestParam(defaultValue = "30") int days) {
        ClickAnalyticsResponse analytics = clickTrackingService.getAnalytics(days);
        return ResponseEntity.ok(analytics);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return null; // Allow anonymous clicks
        }
        return 1L; // Placeholder - should be extracted from JWT claims
    }
}
