package com.findoraai.giftfinder.tracking.controller;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.tracking.dto.ClickAnalyticsResponse;
import com.findoraai.giftfinder.tracking.dto.ClickResponse;
import com.findoraai.giftfinder.tracking.dto.CreateClickRequest;
import com.findoraai.giftfinder.tracking.service.ClickTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ClickTrackingController {

    private final ClickTrackingService clickTrackingService;

    @PostMapping("/clicks")
    public ResponseEntity<ClickResponse> createClick(
            @Valid @RequestBody CreateClickRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        // Support both authenticated and anonymous users for click tracking
        // userDetails will be null for anonymous/unauthenticated requests
        Long userId = userDetails != null ? ((User) userDetails).getId() : null;
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
}
