package com.findoraai.giftfinder.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickAnalyticsResponse {
    private Map<String, Long> clicksByProvider;
    private Map<String, Long> clicksByDate;
    private Long totalClicks;
}
