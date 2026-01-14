package com.findoraai.giftfinder.giftcard.service;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.giftcard.dto.CreateGiftCardRequest;
import com.findoraai.giftfinder.giftcard.dto.GiftCardResponse;
import com.findoraai.giftfinder.giftcard.model.GiftCard;

import java.util.List;

public interface GiftCardService {
    /**
     * Create a new gift card
     * @param request The request
     * @param sender The sender user
     * @return The created gift card
     */
    GiftCardResponse createGiftCard(CreateGiftCardRequest request, User sender);

    /**
     * Send a gift card to the recipient
     * @param code The gift card code
     */
    void sendGiftCard(String code);

    /**
     * Redeem a gift card
     * @param code The gift card code
     * @param redeemer The user redeeming the gift card
     * @return The redeemed gift card
     */
    GiftCardResponse redeemGiftCard(String code, User redeemer);

    /**
     * Cancel a gift card (admin only)
     * @param id The gift card ID
     */
    void cancelGiftCard(Long id);

    /**
     * Expire a gift card (admin only)
     * @param id The gift card ID
     */
    void expireGiftCard(Long id);

    /**
     * Get gift cards sent by a user
     * @param user The user
     * @return List of gift cards
     */
    List<GiftCardResponse> getGiftCardsBySender(User user);

    /**
     * Get gift card by code (without hashing check)
     * @param code The code
     * @return The gift card
     */
    GiftCard getByCode(String code);

    /**
     * Process expired gift cards (scheduled job)
     */
    void processExpiredGiftCards();
}
