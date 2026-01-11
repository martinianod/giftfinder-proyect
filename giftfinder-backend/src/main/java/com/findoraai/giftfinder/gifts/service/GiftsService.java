package com.findoraai.giftfinder.gifts.service;

import com.findoraai.giftfinder.gifts.dto.GiftResponse;
import com.findoraai.giftfinder.gifts.dto.GiftSearchResponse;

import java.util.List;

public interface GiftsService {

    GiftSearchResponse search(String query);
}
