package com.findoraai.giftfinder.notifications.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SavedProductResponse(
    Long id,
    String productId,
    String title,
    BigDecimal currentPrice,
    String currency,
    String productUrl,
    String imageUrl,
    String store,
    Long recipientId,
    String recipientName,
    Boolean priceTrackingEnabled,
    BigDecimal priceDropThresholdPercent,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
