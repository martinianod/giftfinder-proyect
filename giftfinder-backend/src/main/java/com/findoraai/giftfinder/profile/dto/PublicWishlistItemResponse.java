package com.findoraai.giftfinder.profile.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicWishlistItemResponse {

    private String title;
    private String description;
    private String url;
    private BigDecimal price;
    private String currency;
    private String imageUrl;
    private Integer priority;
    private boolean purchased;
}
