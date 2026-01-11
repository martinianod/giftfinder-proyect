package com.findoraai.giftfinder.gifts.controller;

import com.findoraai.giftfinder.gifts.dto.GiftRequest;
import com.findoraai.giftfinder.gifts.dto.GiftResponse;
import com.findoraai.giftfinder.gifts.dto.GiftSearchResponse;
import com.findoraai.giftfinder.gifts.service.GiftsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gifts")
@RequiredArgsConstructor
public class GiftsController {
    private final GiftsService giftsService;

    @PostMapping("/search")
    public ResponseEntity<GiftSearchResponse> search(@RequestBody GiftRequest request) {
        GiftSearchResponse results = giftsService.search(request.query());
        return ResponseEntity.ok(results);
    }
}
