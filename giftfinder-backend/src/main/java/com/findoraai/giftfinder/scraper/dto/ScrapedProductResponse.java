package com.findoraai.giftfinder.scraper.dto;

import lombok.Data;

import java.util.List;

@Data
public class ScrapedProductResponse {
    private String id;
    private String title;
    private String description;
    private Double price;
    private String currency;
    private String image_url;
    private String product_url;
    private String store;
    private Double rating;
    private List<String> tags;
}
