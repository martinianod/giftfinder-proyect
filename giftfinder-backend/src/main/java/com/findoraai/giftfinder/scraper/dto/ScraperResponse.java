package com.findoraai.giftfinder.scraper.dto;

import lombok.Data;

import java.util.List;

@Data
public class ScraperResponse {

    private InterpretedIntent interpretedIntent;
    private List<ScrapedProductResponse> recommendations;

}