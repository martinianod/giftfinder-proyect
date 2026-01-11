package com.findoraai.giftfinder.gifts.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.findoraai.giftfinder.gifts.dto.ParsedQuery;
import com.findoraai.giftfinder.gifts.service.AIQueryParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AIQueryParserServiceImpl implements AIQueryParserService {

    private final WebClient webClient; // ESTE CLIENTE APUNTA AL SCRAPER

    public AIQueryParserServiceImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public ParsedQuery parseQuery(String input) {

        try {
            log.info("Sending query to scraper: {}", input);

            JsonNode response = webClient.post()
                    .uri("/parse-query") // FASTAPI endpoint
                    .bodyValue(Map.of("query", input))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            log.info("Scraper response: {}", response);

            return new ParsedQuery(
                    response.path("recipientType").asText(null),
                    response.path("age").isInt() ? response.get("age").asInt() : null,
                    response.path("budgetMin").isInt() ? response.get("budgetMin").asInt() : null,
                    response.path("budgetMax").isInt() ? response.get("budgetMax").asInt() : null,
                    new ObjectMapper().convertValue(
                            response.get("interests"),
                            new TypeReference<List<String>>() {})
            );

        } catch (Exception e) {
            log.error("Scraper parsing failed — using fallback", e);

            return new ParsedQuery(
                    "unknown",
                    null,
                    null,
                    null,
                    List.of()
            );
        }
    }
}

