package com.findoraai.giftfinder.giftcard.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGiftCardRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
    private String currency;

    @Size(max = 500, message = "Message cannot exceed 500 characters")
    private String message;

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String recipientEmail;

    private LocalDate deliveryDate;

    @Positive(message = "Expiry months must be positive")
    @Max(value = 60, message = "Expiry months cannot exceed 60")
    private Integer expiryMonths;
}
