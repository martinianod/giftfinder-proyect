package com.findoraai.giftfinder.notifications.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record SavedProductRequest(
    @NotBlank(message = "Product ID is required")
    String productId,
    
    @NotBlank(message = "Title is required")
    String title,
    
    @NotNull(message = "Current price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    BigDecimal currentPrice,
    
    @NotBlank(message = "Currency is required")
    String currency,
    
    String productUrl,
    
    String imageUrl,
    
    String store,
    
    Long recipientId,
    
    Boolean priceTrackingEnabled,
    
    @DecimalMin(value = "0.0", message = "Threshold must be at least 0")
    @DecimalMax(value = "100.0", message = "Threshold must be at most 100")
    BigDecimal priceDropThresholdPercent
) {}
