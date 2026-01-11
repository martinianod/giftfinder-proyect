package com.findoraai.giftfinder.gifts.dto;
import java.util.List;

public record ParsedQuery(
        String recipientType,
        Integer age,
        Integer budgetMin,
        Integer budgetMax,
        List<String> interests
) {}
