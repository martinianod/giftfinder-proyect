package com.findoraai.giftfinder.profile.service;

import com.findoraai.giftfinder.profile.dto.PublicProfileResponse;
import com.findoraai.giftfinder.profile.dto.PublicWishlistItemResponse;
import com.findoraai.giftfinder.profile.model.RecipientProfile;
import com.findoraai.giftfinder.profile.model.ShareLinkToken;
import com.findoraai.giftfinder.profile.repository.ShareLinkTokenRepository;
import com.findoraai.giftfinder.scraper.dto.ScraperResponse;
import com.findoraai.giftfinder.scraper.service.ScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicProfileServiceImpl implements PublicProfileService {

    private final ShareLinkTokenRepository tokenRepository;
    private final ScraperService scraperService;

    @Override
    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(String token) {
        ShareLinkToken shareToken = tokenRepository.findByHashedToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired share link"));

        if (shareToken.isExpired()) {
            throw new RuntimeException("Share link has expired");
        }

        RecipientProfile profile = shareToken.getProfile();

        if (profile.getVisibility() != RecipientProfile.ProfileVisibility.SHARED_LINK) {
            throw new RuntimeException("Profile is not publicly accessible");
        }

        List<PublicWishlistItemResponse> wishlistItems = profile.getWishlistItems().stream()
                .map(item -> PublicWishlistItemResponse.builder()
                        .title(item.getTitle())
                        .description(item.getDescription())
                        .url(item.getUrl())
                        .price(item.getPrice())
                        .currency(item.getCurrency())
                        .imageUrl(item.getImageUrl())
                        .priority(item.getPriority())
                        .purchased(item.isPurchased())
                        .build())
                .collect(Collectors.toList());

        return PublicProfileResponse.builder()
                .name(profile.getName())
                .relationship(profile.getRelationship())
                .interests(profile.getInterests())
                .restrictions(profile.getRestrictions())
                .sizes(profile.getSizes())
                .preferredStores(profile.getPreferredStores())
                .budgetMin(profile.getBudgetMin())
                .budgetMax(profile.getBudgetMax())
                .wishlistItems(wishlistItems)
                .claimed(profile.isClaimed())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ScraperResponse getGiftRecommendations(String token) {
        ShareLinkToken shareToken = tokenRepository.findByHashedToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired share link"));

        if (shareToken.isExpired()) {
            throw new RuntimeException("Share link has expired");
        }

        RecipientProfile profile = shareToken.getProfile();

        if (profile.getVisibility() != RecipientProfile.ProfileVisibility.SHARED_LINK) {
            throw new RuntimeException("Profile is not publicly accessible");
        }

        // Build query from profile interests and other data
        StringBuilder query = new StringBuilder();
        
        if (profile.getInterests() != null && !profile.getInterests().isEmpty()) {
            query.append(profile.getInterests());
        }
        
        if (profile.getRelationship() != null && !profile.getRelationship().isEmpty()) {
            query.append(" para ").append(profile.getRelationship());
        }

        if (query.length() == 0) {
            query.append("regalo para ").append(profile.getName());
        }

        log.info("Generating gift recommendations for profile: {} with query: {}", profile.getName(), query);

        return scraperService.search(query.toString());
    }
}
