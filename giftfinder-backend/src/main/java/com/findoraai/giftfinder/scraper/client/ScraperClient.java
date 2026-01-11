package com.findoraai.giftfinder.scraper.client;

import com.findoraai.giftfinder.scraper.dto.ScraperResponse;
import com.findoraai.giftfinder.scraper.dto.UniversalScrapeRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class ScraperClient {

    private final WebClient webClient;

    public ScraperClient(@Qualifier("scraperWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public ScraperResponse search(UniversalScrapeRequest request) {
        Map<String, String> body = Map.of("query", request.getQueryContext());

        return webClient.post()
                .uri("/scrape/search")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(ScraperResponse.class)
                .block();
    }
}

