package com.findoraai.giftfinder.profile.service;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.profile.dto.WishlistItemRequest;
import com.findoraai.giftfinder.profile.dto.WishlistItemResponse;

import java.util.List;

public interface WishlistService {
    List<WishlistItemResponse> getWishlistItems(Long profileId, User user);
    WishlistItemResponse getWishlistItem(Long profileId, Long itemId, User user);
    WishlistItemResponse addWishlistItem(Long profileId, WishlistItemRequest request, User user);
    WishlistItemResponse updateWishlistItem(Long profileId, Long itemId, WishlistItemRequest request, User user);
    void deleteWishlistItem(Long profileId, Long itemId, User user);
}
