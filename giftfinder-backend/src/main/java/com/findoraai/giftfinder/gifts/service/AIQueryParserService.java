package com.findoraai.giftfinder.gifts.service;

import com.findoraai.giftfinder.gifts.dto.ParsedQuery;

public interface AIQueryParserService {
    ParsedQuery parseQuery(String input);
}
