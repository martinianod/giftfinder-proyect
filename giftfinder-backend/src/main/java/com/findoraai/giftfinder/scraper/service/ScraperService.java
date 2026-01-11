package com.findoraai.giftfinder.scraper.service;

import com.findoraai.giftfinder.scraper.client.ScraperClient;
import com.findoraai.giftfinder.scraper.dto.ScraperResponse;
import com.findoraai.giftfinder.scraper.dto.UniversalScrapeRequest;
import com.findoraai.giftfinder.scraper.dto.ScrapedProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

public interface ScraperService {
    ScraperResponse search(String query);
}
