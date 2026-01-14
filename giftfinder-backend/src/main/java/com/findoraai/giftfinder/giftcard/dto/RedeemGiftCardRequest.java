package com.findoraai.giftfinder.giftcard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedeemGiftCardRequest {

    @NotBlank(message = "Gift card code is required")
    private String code;
}
