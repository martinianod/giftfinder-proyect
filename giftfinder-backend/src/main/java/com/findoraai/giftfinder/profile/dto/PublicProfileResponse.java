package com.findoraai.giftfinder.profile.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicProfileResponse {

    private String name;
    private String relationship;
    private String interests;
    private String restrictions;
    private String sizes;
    private String preferredStores;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private List<PublicWishlistItemResponse> wishlistItems;
    private boolean claimed;
}
