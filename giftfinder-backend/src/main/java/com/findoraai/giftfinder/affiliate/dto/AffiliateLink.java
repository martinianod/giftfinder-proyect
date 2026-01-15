package com.findoraai.giftfinder.affiliate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AffiliateLink {
    private String originalUrl;
    private String affiliateUrl;
    private String providerName;
    private String campaignId;
    private String trackingCode;
}
