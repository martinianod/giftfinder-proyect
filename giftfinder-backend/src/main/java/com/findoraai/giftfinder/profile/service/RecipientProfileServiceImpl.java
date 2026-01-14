package com.findoraai.giftfinder.profile.service;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.profile.dto.*;
import com.findoraai.giftfinder.profile.model.*;
import com.findoraai.giftfinder.profile.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipientProfileServiceImpl implements RecipientProfileService {

    private final RecipientProfileRepository profileRepository;
    private final ShareLinkTokenRepository tokenRepository;
    private final WishlistItemRepository wishlistRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @Override
    @Transactional(readOnly = true)
    public List<RecipientProfileResponse> getUserProfiles(User user) {
        List<RecipientProfile> profiles = profileRepository.findByUserOrderByCreatedAtDesc(user);
        return profiles.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RecipientProfileResponse getProfile(Long id, User user) {
        RecipientProfile profile = profileRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        return mapToResponse(profile);
    }

    @Override
    @Transactional
    public RecipientProfileResponse createProfile(RecipientProfileRequest request, User user) {
        RecipientProfile profile = RecipientProfile.builder()
                .user(user)
                .name(request.getName())
                .relationship(request.getRelationship())
                .interests(request.getInterests())
                .restrictions(request.getRestrictions())
                .sizes(request.getSizes())
                .preferredStores(request.getPreferredStores())
                .budgetMin(request.getBudgetMin())
                .budgetMax(request.getBudgetMax())
                .visibility(request.getVisibility())
                .claimEmail(request.getClaimEmail())
                .claimed(false)
                .build();

        profile = profileRepository.save(profile);

        // Generate share token if visibility is SHARED_LINK
        if (profile.getVisibility() == RecipientProfile.ProfileVisibility.SHARED_LINK) {
            generateAndSaveToken(profile);
        }

        return mapToResponse(profile);
    }

    @Override
    @Transactional
    public RecipientProfileResponse updateProfile(Long id, RecipientProfileRequest request, User user) {
        RecipientProfile profile = profileRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        profile.setName(request.getName());
        profile.setRelationship(request.getRelationship());
        profile.setInterests(request.getInterests());
        profile.setRestrictions(request.getRestrictions());
        profile.setSizes(request.getSizes());
        profile.setPreferredStores(request.getPreferredStores());
        profile.setBudgetMin(request.getBudgetMin());
        profile.setBudgetMax(request.getBudgetMax());
        
        // Handle visibility change
        RecipientProfile.ProfileVisibility oldVisibility = profile.getVisibility();
        profile.setVisibility(request.getVisibility());

        if (request.getClaimEmail() != null) {
            profile.setClaimEmail(request.getClaimEmail());
        }

        // Generate token if changed to SHARED_LINK
        if (oldVisibility != RecipientProfile.ProfileVisibility.SHARED_LINK 
                && request.getVisibility() == RecipientProfile.ProfileVisibility.SHARED_LINK) {
            generateAndSaveToken(profile);
        }

        // Remove token if changed to PRIVATE
        if (oldVisibility == RecipientProfile.ProfileVisibility.SHARED_LINK 
                && request.getVisibility() == RecipientProfile.ProfileVisibility.PRIVATE) {
            tokenRepository.findByProfile(profile).ifPresent(tokenRepository::delete);
        }

        profile = profileRepository.save(profile);
        return mapToResponse(profile);
    }

    @Override
    @Transactional
    public void deleteProfile(Long id, User user) {
        RecipientProfile profile = profileRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        profileRepository.delete(profile);
    }

    @Override
    @Transactional
    public String generateShareLink(Long id, User user) {
        RecipientProfile profile = profileRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        if (profile.getVisibility() != RecipientProfile.ProfileVisibility.SHARED_LINK) {
            throw new RuntimeException("Profile must have SHARED_LINK visibility");
        }

        // Check if token already exists
        ShareLinkToken existingToken = tokenRepository.findByProfile(profile).orElse(null);
        if (existingToken != null && !existingToken.isExpired()) {
            return buildShareUrl(existingToken.getHashedToken());
        }

        // Generate new token
        ShareLinkToken token = generateAndSaveToken(profile);
        return buildShareUrl(token.getHashedToken());
    }

    private ShareLinkToken generateAndSaveToken(RecipientProfile profile) {
        // Generate a secure random token (32 bytes = 256 bits)
        SecureRandom random = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        // Hash the token for storage
        String hashedToken = passwordEncoder.encode(rawToken);

        // Delete existing token if any
        tokenRepository.findByProfile(profile).ifPresent(tokenRepository::delete);

        ShareLinkToken token = ShareLinkToken.builder()
                .profile(profile)
                .hashedToken(rawToken) // Store raw token for URL, not hashed (for this simple implementation)
                .build();

        return tokenRepository.save(token);
    }

    private String buildShareUrl(String token) {
        return appBaseUrl + "/public/recipient/" + token;
    }

    private RecipientProfileResponse mapToResponse(RecipientProfile profile) {
        String shareUrl = null;
        if (profile.getVisibility() == RecipientProfile.ProfileVisibility.SHARED_LINK 
                && profile.getShareToken() != null) {
            shareUrl = buildShareUrl(profile.getShareToken().getHashedToken());
        }

        List<WishlistItemResponse> wishlistItems = profile.getWishlistItems().stream()
                .map(item -> WishlistItemResponse.builder()
                        .id(item.getId())
                        .title(item.getTitle())
                        .description(item.getDescription())
                        .url(item.getUrl())
                        .price(item.getPrice())
                        .currency(item.getCurrency())
                        .imageUrl(item.getImageUrl())
                        .priority(item.getPriority())
                        .purchased(item.isPurchased())
                        .createdAt(item.getCreatedAt())
                        .updatedAt(item.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        return RecipientProfileResponse.builder()
                .id(profile.getId())
                .name(profile.getName())
                .relationship(profile.getRelationship())
                .interests(profile.getInterests())
                .restrictions(profile.getRestrictions())
                .sizes(profile.getSizes())
                .preferredStores(profile.getPreferredStores())
                .budgetMin(profile.getBudgetMin())
                .budgetMax(profile.getBudgetMax())
                .visibility(profile.getVisibility())
                .shareUrl(shareUrl)
                .claimed(profile.isClaimed())
                .claimEmail(profile.getClaimEmail())
                .wishlistItems(wishlistItems)
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
