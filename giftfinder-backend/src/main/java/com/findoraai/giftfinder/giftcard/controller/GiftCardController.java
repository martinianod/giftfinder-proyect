package com.findoraai.giftfinder.giftcard.controller;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.giftcard.dto.CreateGiftCardRequest;
import com.findoraai.giftfinder.giftcard.dto.GiftCardResponse;
import com.findoraai.giftfinder.giftcard.dto.RedeemGiftCardRequest;
import com.findoraai.giftfinder.giftcard.service.GiftCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/giftcards")
@RequiredArgsConstructor
public class GiftCardController {

    private final GiftCardService giftCardService;

    /**
     * Create a new gift card
     * POST /api/giftcards
     */
    @PostMapping
    public ResponseEntity<GiftCardResponse> createGiftCard(
            @Valid @RequestBody CreateGiftCardRequest request,
            @AuthenticationPrincipal User user) {
        GiftCardResponse response = giftCardService.createGiftCard(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Redeem a gift card
     * POST /api/giftcards/redeem
     */
    @PostMapping("/redeem")
    public ResponseEntity<GiftCardResponse> redeemGiftCard(
            @Valid @RequestBody RedeemGiftCardRequest request,
            @AuthenticationPrincipal User user) {
        GiftCardResponse response = giftCardService.redeemGiftCard(request.getCode(), user);
        return ResponseEntity.ok(response);
    }

    /**
     * Get gift cards sent by the current user
     * GET /api/giftcards
     */
    @GetMapping
    public ResponseEntity<List<GiftCardResponse>> getMyGiftCards(
            @AuthenticationPrincipal User user) {
        List<GiftCardResponse> giftCards = giftCardService.getGiftCardsBySender(user);
        return ResponseEntity.ok(giftCards);
    }
}
