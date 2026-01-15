package com.findoraai.giftfinder.profile.controller;

import com.findoraai.giftfinder.profile.dto.PublicProfileResponse;
import com.findoraai.giftfinder.profile.service.PublicProfileService;
import com.findoraai.giftfinder.scraper.dto.ScraperResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/recipient")
@RequiredArgsConstructor
public class PublicProfileController {

    private final PublicProfileService publicProfileService;

    @GetMapping("/{token}")
    public ResponseEntity<PublicProfileResponse> getPublicProfile(@PathVariable String token) {
        PublicProfileResponse profile = publicProfileService.getPublicProfile(token);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/{token}/recommendations")
    public ResponseEntity<ScraperResponse> getRecommendations(@PathVariable String token) {
        ScraperResponse recommendations = publicProfileService.getGiftRecommendations(token);
        return ResponseEntity.ok(recommendations);
    }
}
