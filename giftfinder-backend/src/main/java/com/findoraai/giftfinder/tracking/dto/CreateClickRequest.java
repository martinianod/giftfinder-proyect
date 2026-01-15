package com.findoraai.giftfinder.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClickRequest {
    private String productId;
    private String provider;
    private String targetUrl;
    private String anonymousId;
    private String campaignId;
    private String trackingTags;
    private String utmSource;
    private String utmMedium;
    private String utmCampaign;
    private String utmContent;
    private String utmTerm;
}
