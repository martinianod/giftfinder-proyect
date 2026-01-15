package com.findoraai.giftfinder.tracking.service;

import com.findoraai.giftfinder.tracking.dto.ClickAnalyticsResponse;
import com.findoraai.giftfinder.tracking.dto.ClickResponse;
import com.findoraai.giftfinder.tracking.dto.CreateClickRequest;

public interface ClickTrackingService {
    ClickResponse createClick(CreateClickRequest request, Long userId);
    String getRedirectUrl(String clickId);
    ClickAnalyticsResponse getAnalytics(int days);
}
