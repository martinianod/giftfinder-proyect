package com.findoraai.giftfinder.gifts.dto;

import lombok.Data;

import java.util.List;

@Data
public class GiftSearchResponse {
    private InterpretedIntent interpretedIntent;
    private List<GiftResponse> recommendations;

    @Data
    public static class InterpretedIntent {
        private String recipient;
        private Integer age;
        private Double budgetMin;
        private Double budgetMax;
        private List<String> interests;
    }
}
