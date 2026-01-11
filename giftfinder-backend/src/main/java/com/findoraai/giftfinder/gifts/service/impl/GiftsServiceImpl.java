package com.findoraai.giftfinder.gifts.service.impl;

import com.findoraai.giftfinder.gifts.dto.GiftResponse;
import com.findoraai.giftfinder.gifts.dto.GiftSearchResponse;
import com.findoraai.giftfinder.gifts.service.GiftsService;
import com.findoraai.giftfinder.scraper.service.ScraperService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GiftsServiceImpl implements GiftsService {

    private final ScraperService scraperService;

    @Override
    public GiftSearchResponse search(String query) {

        var scraperResponse = scraperService.search(query);

        // Convertir productos
        var giftRecommendations = scraperResponse.getRecommendations()
                .stream()
                .map(p -> new GiftResponse(
                        null,
                        p.getTitle(),
                        p.getDescription(),
                        p.getPrice() != null ? p.getPrice().intValue() : 0,
                        p.getCurrency(),
                        p.getImage_url(),
                        p.getProduct_url(),
                        p.getStore(),
                        p.getRating() != null ? p.getRating() : 0,
                        p.getTags()
                ))
                .toList();

        // Convertir interpretedIntent
        GiftSearchResponse.InterpretedIntent intent = new GiftSearchResponse.InterpretedIntent();
        if (scraperResponse.getInterpretedIntent() != null) {
            var src = scraperResponse.getInterpretedIntent();
            intent.setRecipient(src.getRecipient());
            intent.setAge(src.getAge());
            intent.setBudgetMin(src.getBudgetMin());
            intent.setBudgetMax(src.getBudgetMax());
            intent.setInterests(src.getInterests());
        }

        // Armar response final
        GiftSearchResponse response = new GiftSearchResponse();
        response.setRecommendations(giftRecommendations);
        response.setInterpretedIntent(intent);

        return response;
    }
}



