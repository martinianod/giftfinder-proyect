package com.findoraai.giftfinder.profile.controller;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.profile.dto.WishlistItemRequest;
import com.findoraai.giftfinder.profile.dto.WishlistItemResponse;
import com.findoraai.giftfinder.profile.service.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profiles/{profileId}/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<List<WishlistItemResponse>> getWishlistItems(
            @PathVariable Long profileId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        List<WishlistItemResponse> items = wishlistService.getWishlistItems(profileId, user);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<WishlistItemResponse> getWishlistItem(
            @PathVariable Long profileId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        WishlistItemResponse item = wishlistService.getWishlistItem(profileId, itemId, user);
        return ResponseEntity.ok(item);
    }

    @PostMapping
    public ResponseEntity<WishlistItemResponse> addWishlistItem(
            @PathVariable Long profileId,
            @Valid @RequestBody WishlistItemRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        WishlistItemResponse item = wishlistService.addWishlistItem(profileId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<WishlistItemResponse> updateWishlistItem(
            @PathVariable Long profileId,
            @PathVariable Long itemId,
            @Valid @RequestBody WishlistItemRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        WishlistItemResponse item = wishlistService.updateWishlistItem(profileId, itemId, request, user);
        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteWishlistItem(
            @PathVariable Long profileId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        wishlistService.deleteWishlistItem(profileId, itemId, user);
        return ResponseEntity.noContent().build();
    }
}
