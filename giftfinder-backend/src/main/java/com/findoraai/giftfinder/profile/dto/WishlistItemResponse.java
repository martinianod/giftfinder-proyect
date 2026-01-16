package com.findoraai.giftfinder.profile.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItemResponse {

    private Long id;
    private String title;
    private String description;
    private String url;
    private BigDecimal price;
    private String currency;
    private String imageUrl;
    private Integer priority;
    private boolean purchased;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
