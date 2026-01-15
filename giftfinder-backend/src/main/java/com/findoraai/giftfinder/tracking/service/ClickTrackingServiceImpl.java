package com.findoraai.giftfinder.tracking.service;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.auth.repository.UserRepository;
import com.findoraai.giftfinder.tracking.dto.ClickAnalyticsResponse;
import com.findoraai.giftfinder.tracking.dto.ClickResponse;
import com.findoraai.giftfinder.tracking.dto.CreateClickRequest;
import com.findoraai.giftfinder.tracking.model.OutboundClick;
import com.findoraai.giftfinder.tracking.repository.OutboundClickRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClickTrackingServiceImpl implements ClickTrackingService {

    private final OutboundClickRepository clickRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ClickResponse createClick(CreateClickRequest request, Long userId) {
        String clickId = UUID.randomUUID().toString();

        OutboundClick.OutboundClickBuilder clickBuilder = OutboundClick.builder()
                .clickId(clickId)
                .productId(request.getProductId())
                .provider(request.getProvider())
                .targetUrl(request.getTargetUrl())
                .anonymousId(request.getAnonymousId())
                .campaignId(request.getCampaignId())
                .trackingTags(request.getTrackingTags())
                .utmSource(request.getUtmSource())
                .utmMedium(request.getUtmMedium())
                .utmCampaign(request.getUtmCampaign())
                .utmContent(request.getUtmContent())
                .utmTerm(request.getUtmTerm());

        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            clickBuilder.user(user);
        }

        OutboundClick click = clickBuilder.build();
        clickRepository.save(click);

        // Build redirect URL with UTM parameters
        String redirectUrl = buildRedirectUrl(clickId);

        return ClickResponse.builder()
                .clickId(clickId)
                .redirectUrl(redirectUrl)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String getRedirectUrl(String clickId) {
        OutboundClick click = clickRepository.findByClickId(clickId)
                .orElseThrow(() -> new IllegalArgumentException("Click not found"));

        String targetUrl = click.getTargetUrl();

        // Add UTM parameters if present
        if (hasUtmParameters(click)) {
            targetUrl = appendUtmParameters(targetUrl, click);
        }

        return targetUrl;
    }

    @Override
    @Transactional(readOnly = true)
    public ClickAnalyticsResponse getAnalytics(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        List<Object[]> providerCounts = clickRepository.countClicksByProvider(startDate);
        List<Object[]> dateCounts = clickRepository.countClicksByDate(startDate);

        Map<String, Long> clicksByProvider = new HashMap<>();
        for (Object[] row : providerCounts) {
            clicksByProvider.put((String) row[0], (Long) row[1]);
        }

        Map<String, Long> clicksByDate = new HashMap<>();
        long totalClicks = 0;
        for (Object[] row : dateCounts) {
            clicksByDate.put(row[0].toString(), (Long) row[1]);
            totalClicks += (Long) row[1];
        }

        return ClickAnalyticsResponse.builder()
                .clicksByProvider(clicksByProvider)
                .clicksByDate(clicksByDate)
                .totalClicks(totalClicks)
                .build();
    }

    private String buildRedirectUrl(String clickId) {
        return "/api/r/" + clickId;
    }

    private boolean hasUtmParameters(OutboundClick click) {
        return click.getUtmSource() != null || 
               click.getUtmMedium() != null || 
               click.getUtmCampaign() != null ||
               click.getUtmContent() != null || 
               click.getUtmTerm() != null;
    }

    private String appendUtmParameters(String url, OutboundClick click) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);

        if (click.getUtmSource() != null) {
            builder.queryParam("utm_source", click.getUtmSource());
        }
        if (click.getUtmMedium() != null) {
            builder.queryParam("utm_medium", click.getUtmMedium());
        }
        if (click.getUtmCampaign() != null) {
            builder.queryParam("utm_campaign", click.getUtmCampaign());
        }
        if (click.getUtmContent() != null) {
            builder.queryParam("utm_content", click.getUtmContent());
        }
        if (click.getUtmTerm() != null) {
            builder.queryParam("utm_term", click.getUtmTerm());
        }

        return builder.build().toUriString();
    }
}
