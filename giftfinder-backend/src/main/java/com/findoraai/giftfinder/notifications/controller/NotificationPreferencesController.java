package com.findoraai.giftfinder.notifications.controller;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.notifications.dto.NotificationPreferencesRequest;
import com.findoraai.giftfinder.notifications.dto.NotificationPreferencesResponse;
import com.findoraai.giftfinder.notifications.service.NotificationPreferencesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification-preferences")
@RequiredArgsConstructor
public class NotificationPreferencesController {

    private final NotificationPreferencesService preferencesService;

    @GetMapping
    public ResponseEntity<NotificationPreferencesResponse> getUserPreferences(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        NotificationPreferencesResponse preferences = preferencesService.getUserPreferences(user);
        return ResponseEntity.ok(preferences);
    }

    @PutMapping
    public ResponseEntity<NotificationPreferencesResponse> updatePreferences(
            @Valid @RequestBody NotificationPreferencesRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        NotificationPreferencesResponse preferences = preferencesService.updatePreferences(request, user);
        return ResponseEntity.ok(preferences);
    }
}
