package com.findoraai.giftfinder.profile.controller;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.profile.dto.RecipientProfileRequest;
import com.findoraai.giftfinder.profile.dto.RecipientProfileResponse;
import com.findoraai.giftfinder.profile.service.RecipientProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class RecipientProfileController {

    private final RecipientProfileService profileService;

    @GetMapping
    public ResponseEntity<List<RecipientProfileResponse>> getUserProfiles(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        List<RecipientProfileResponse> profiles = profileService.getUserProfiles(user);
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipientProfileResponse> getProfile(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        RecipientProfileResponse profile = profileService.getProfile(id, user);
        return ResponseEntity.ok(profile);
    }

    @PostMapping
    public ResponseEntity<RecipientProfileResponse> createProfile(
            @Valid @RequestBody RecipientProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        RecipientProfileResponse profile = profileService.createProfile(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(profile);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecipientProfileResponse> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody RecipientProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        RecipientProfileResponse profile = profileService.updateProfile(id, request, user);
        return ResponseEntity.ok(profile);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        profileService.deleteProfile(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<Map<String, String>> generateShareLink(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        String shareUrl = profileService.generateShareLink(id, user);
        return ResponseEntity.ok(Map.of("shareUrl", shareUrl));
    }
}
