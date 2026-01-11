package com.findoraai.giftfinder.gifts.service;

import com.findoraai.giftfinder.gifts.dto.GiftResponse;
import com.findoraai.giftfinder.gifts.dto.ParsedQuery;

import java.util.List;

public interface ProductProviderService {
    List<GiftResponse> findProducts(ParsedQuery parsed);
}
