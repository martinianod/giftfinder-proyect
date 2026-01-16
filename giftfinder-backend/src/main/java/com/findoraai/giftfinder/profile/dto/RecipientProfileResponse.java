package com.findoraai.giftfinder.profile.dto;

import com.findoraai.giftfinder.profile.model.RecipientProfile;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipientProfileResponse {

    private Long id;
    private String name;
    private String relationship;
    private String interests;
    private String restrictions;
    private String sizes;
    private String preferredStores;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private RecipientProfile.ProfileVisibility visibility;
    private String shareUrl;
    private boolean claimed;
    private String claimEmail;
    private List<WishlistItemResponse> wishlistItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
