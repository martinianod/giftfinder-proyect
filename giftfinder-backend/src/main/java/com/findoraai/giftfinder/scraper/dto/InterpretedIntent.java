package com.findoraai.giftfinder.scraper.dto;

import lombok.Data;

import java.util.List;

@Data
public class InterpretedIntent {
    private String recipient;
    private Integer age;
    private Double budgetMin;
    private Double budgetMax;
    private List<String> interests;
}
