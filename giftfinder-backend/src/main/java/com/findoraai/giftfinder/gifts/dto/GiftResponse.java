package com.findoraai.giftfinder.gifts.dto;

import java.util.List;

public record GiftResponse(
        String id,
        String title,
        String description,
        Integer price,
        String currency,
        String imageUrl,
        String productUrl,
        String store,
        Double rating,
        List<String> tags
) {}
