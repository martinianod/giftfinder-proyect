package com.findoraai.giftfinder.giftcard.dto;

import com.findoraai.giftfinder.giftcard.model.GiftCard;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GiftCardResponse {
    private Long id;
    private String code;
    private BigDecimal amount;
    private String currency;
    private String message;
    private String recipientEmail;
    private LocalDate deliveryDate;
    private GiftCard.GiftCardStatus status;
    private LocalDate expiryDate;
    private LocalDateTime createdAt;
    private LocalDateTime redeemedAt;
    
    public static GiftCardResponse fromEntity(GiftCard giftCard) {
        return GiftCardResponse.builder()
            .id(giftCard.getId())
            .code(giftCard.getCode())
            .amount(giftCard.getAmount())
            .currency(giftCard.getCurrency())
            .message(giftCard.getMessage())
            .recipientEmail(giftCard.getRecipientEmail())
            .deliveryDate(giftCard.getDeliveryDate())
            .status(giftCard.getStatus())
            .expiryDate(giftCard.getExpiryDate())
            .createdAt(giftCard.getCreatedAt())
            .redeemedAt(giftCard.getRedeemedAt())
            .build();
    }
}
