package com.findoraai.giftfinder.scraper.service.impl;

import com.findoraai.giftfinder.scraper.client.ScraperClient;
import com.findoraai.giftfinder.scraper.dto.ScraperResponse;
import com.findoraai.giftfinder.scraper.dto.UniversalScrapeRequest;
import com.findoraai.giftfinder.scraper.service.ScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScraperServiceImpl implements ScraperService {

    private final ScraperClient scraperClient;

    @Override
    public ScraperResponse search(String query) {
        log.info("Scraper search query='{}'", query);
        var request = UniversalScrapeRequest.builder().queryContext(query).build();

        ScraperResponse resp = scraperClient.search(request);

        int count = resp != null && resp.getRecommendations() != null ? resp.getRecommendations().size() : 0;
        log.info("Scraper response count={}", count);

        return resp;
    }
}

