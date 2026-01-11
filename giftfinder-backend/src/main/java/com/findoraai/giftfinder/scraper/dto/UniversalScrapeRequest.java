package com.findoraai.giftfinder.scraper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UniversalScrapeRequest {
    private String url;
    private String queryContext;
    private Integer maxProducts;
}
