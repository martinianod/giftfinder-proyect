package com.findoraai.giftfinder.affiliate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AffiliateProduct {
    private String id;
    private String title;
    private String description;
    private BigDecimal price;
    private String currency;
    private String imageUrl;
    private String affiliateUrl;
    private String providerName;
    private Double commissionRate;
    private BigDecimal estimatedCommission;
}
