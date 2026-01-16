package com.findoraai.giftfinder.profile.service;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.profile.dto.WishlistItemRequest;
import com.findoraai.giftfinder.profile.dto.WishlistItemResponse;
import com.findoraai.giftfinder.profile.model.RecipientProfile;
import com.findoraai.giftfinder.profile.model.WishlistItem;
import com.findoraai.giftfinder.profile.repository.RecipientProfileRepository;
import com.findoraai.giftfinder.profile.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistServiceImpl implements WishlistService {

    private final WishlistItemRepository wishlistRepository;
    private final RecipientProfileRepository profileRepository;

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlistItems(Long profileId, User user) {
        RecipientProfile profile = profileRepository.findByIdAndUser(profileId, user)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        
        List<WishlistItem> items = wishlistRepository.findByProfileOrderByPriorityDescCreatedAtDesc(profile);
        return items.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public WishlistItemResponse getWishlistItem(Long profileId, Long itemId, User user) {
        RecipientProfile profile = profileRepository.findByIdAndUser(profileId, user)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        
        WishlistItem item = wishlistRepository.findByIdAndProfile(itemId, profile)
                .orElseThrow(() -> new RuntimeException("Wishlist item not found"));
        
        return mapToResponse(item);
    }

    @Override
    @Transactional
    public WishlistItemResponse addWishlistItem(Long profileId, WishlistItemRequest request, User user) {
        RecipientProfile profile = profileRepository.findByIdAndUser(profileId, user)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        WishlistItem item = WishlistItem.builder()
                .profile(profile)
                .title(request.getTitle())
                .description(request.getDescription())
                .url(request.getUrl())
                .price(request.getPrice())
                .currency(request.getCurrency() != null ? request.getCurrency() : "ARS")
                .imageUrl(request.getImageUrl())
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .purchased(request.isPurchased())
                .build();

        item = wishlistRepository.save(item);
        return mapToResponse(item);
    }

    @Override
    @Transactional
    public WishlistItemResponse updateWishlistItem(Long profileId, Long itemId, WishlistItemRequest request, User user) {
        RecipientProfile profile = profileRepository.findByIdAndUser(profileId, user)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        
        WishlistItem item = wishlistRepository.findByIdAndProfile(itemId, profile)
                .orElseThrow(() -> new RuntimeException("Wishlist item not found"));

        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setUrl(request.getUrl());
        item.setPrice(request.getPrice());
        item.setCurrency(request.getCurrency() != null ? request.getCurrency() : item.getCurrency());
        item.setImageUrl(request.getImageUrl());
        item.setPriority(request.getPriority() != null ? request.getPriority() : item.getPriority());
        item.setPurchased(request.isPurchased());

        item = wishlistRepository.save(item);
        return mapToResponse(item);
    }

    @Override
    @Transactional
    public void deleteWishlistItem(Long profileId, Long itemId, User user) {
        RecipientProfile profile = profileRepository.findByIdAndUser(profileId, user)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        
        WishlistItem item = wishlistRepository.findByIdAndProfile(itemId, profile)
                .orElseThrow(() -> new RuntimeException("Wishlist item not found"));

        wishlistRepository.delete(item);
    }

    private WishlistItemResponse mapToResponse(WishlistItem item) {
        return WishlistItemResponse.builder()
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
                .build();
    }
}
