package com.findoraai.giftfinder.profile.service;

import com.findoraai.giftfinder.profile.dto.PublicProfileResponse;
import com.findoraai.giftfinder.scraper.dto.ScraperResponse;

public interface PublicProfileService {
    PublicProfileResponse getPublicProfile(String token);
    ScraperResponse getGiftRecommendations(String token);
}
