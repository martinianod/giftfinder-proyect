package com.findoraai.giftfinder.tracking.service;

import com.findoraai.giftfinder.auth.model.Role;
import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.auth.repository.UserRepository;
import com.findoraai.giftfinder.tracking.dto.ClickAnalyticsResponse;
import com.findoraai.giftfinder.tracking.dto.ClickResponse;
import com.findoraai.giftfinder.tracking.dto.CreateClickRequest;
import com.findoraai.giftfinder.tracking.model.OutboundClick;
import com.findoraai.giftfinder.tracking.repository.OutboundClickRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClickTrackingServiceImplTest {

    @Mock
    private OutboundClickRepository clickRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClickTrackingServiceImpl clickTrackingService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .role(Role.USER)
                .build();
    }

    @Test
    void testCreateClick_WithUser() {
        CreateClickRequest request = CreateClickRequest.builder()
                .productId("PROD-123")
                .provider("mercadolibre")
                .targetUrl("https://mercadolibre.com/product/123")
                .campaignId("test-campaign")
                .utmSource("giftfinder")
                .utmMedium("recommendation")
                .utmCampaign("test-campaign")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(clickRepository.save(any(OutboundClick.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClickResponse response = clickTrackingService.createClick(request, 1L);

        assertNotNull(response);
        assertNotNull(response.getClickId());
        assertTrue(response.getRedirectUrl().contains("/api/r/"));
        assertTrue(response.getRedirectUrl().contains(response.getClickId()));

        verify(clickRepository).save(any(OutboundClick.class));
    }

    @Test
    void testCreateClick_Anonymous() {
        CreateClickRequest request = CreateClickRequest.builder()
                .productId("PROD-123")
                .provider("mercadolibre")
                .targetUrl("https://mercadolibre.com/product/123")
                .anonymousId("anon-uuid-123")
                .build();

        when(clickRepository.save(any(OutboundClick.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClickResponse response = clickTrackingService.createClick(request, null);

        assertNotNull(response);
        assertNotNull(response.getClickId());
        assertTrue(response.getRedirectUrl().contains("/api/r/"));

        verify(clickRepository).save(any(OutboundClick.class));
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void testGetRedirectUrl_WithoutUtm() {
        String clickId = "test-click-id";
        String targetUrl = "https://mercadolibre.com/product/123";

        OutboundClick click = OutboundClick.builder()
                .clickId(clickId)
                .productId("PROD-123")
                .provider("mercadolibre")
                .targetUrl(targetUrl)
                .build();

        when(clickRepository.findByClickId(clickId)).thenReturn(Optional.of(click));

        String redirectUrl = clickTrackingService.getRedirectUrl(clickId);

        assertEquals(targetUrl, redirectUrl);
    }

    @Test
    void testGetRedirectUrl_WithUtm() {
        String clickId = "test-click-id";
        String targetUrl = "https://mercadolibre.com/product/123";

        OutboundClick click = OutboundClick.builder()
                .clickId(clickId)
                .productId("PROD-123")
                .provider("mercadolibre")
                .targetUrl(targetUrl)
                .utmSource("giftfinder")
                .utmMedium("recommendation")
                .utmCampaign("test-campaign")
                .build();

        when(clickRepository.findByClickId(clickId)).thenReturn(Optional.of(click));

        String redirectUrl = clickTrackingService.getRedirectUrl(clickId);

        assertNotNull(redirectUrl);
        assertTrue(redirectUrl.contains("utm_source=giftfinder"));
        assertTrue(redirectUrl.contains("utm_medium=recommendation"));
        assertTrue(redirectUrl.contains("utm_campaign=test-campaign"));
    }

    @Test
    void testGetRedirectUrl_NotFound() {
        String clickId = "non-existent";

        when(clickRepository.findByClickId(clickId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            clickTrackingService.getRedirectUrl(clickId);
        });
    }

    @Test
    void testGetAnalytics() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);

        List<Object[]> providerCounts = Arrays.asList(
                new Object[]{"mercadolibre", 100L},
                new Object[]{"amazon", 75L}
        );

        List<Object[]> dateCounts = Arrays.asList(
                new Object[]{"2026-01-15", 50L},
                new Object[]{"2026-01-14", 45L}
        );

        when(clickRepository.countClicksByProvider(any(LocalDateTime.class)))
                .thenReturn(providerCounts);
        when(clickRepository.countClicksByDate(any(LocalDateTime.class)))
                .thenReturn(dateCounts);

        ClickAnalyticsResponse analytics = clickTrackingService.getAnalytics(30);

        assertNotNull(analytics);
        assertEquals(2, analytics.getClicksByProvider().size());
        assertEquals(100L, analytics.getClicksByProvider().get("mercadolibre"));
        assertEquals(75L, analytics.getClicksByProvider().get("amazon"));
        assertEquals(2, analytics.getClicksByDate().size());
        assertEquals(95L, analytics.getTotalClicks());
    }

    @Test
    void testCreateClick_WithAllUtmParameters() {
        CreateClickRequest request = CreateClickRequest.builder()
                .productId("PROD-123")
                .provider("mercadolibre")
                .targetUrl("https://mercadolibre.com/product/123")
                .utmSource("giftfinder")
                .utmMedium("email")
                .utmCampaign("holiday-2026")
                .utmContent("banner-a")
                .utmTerm("tech-gifts")
                .build();

        when(clickRepository.save(any(OutboundClick.class)))
                .thenAnswer(invocation -> {
                    OutboundClick saved = invocation.getArgument(0);
                    assertEquals("giftfinder", saved.getUtmSource());
                    assertEquals("email", saved.getUtmMedium());
                    assertEquals("holiday-2026", saved.getUtmCampaign());
                    assertEquals("banner-a", saved.getUtmContent());
                    assertEquals("tech-gifts", saved.getUtmTerm());
                    return saved;
                });

        ClickResponse response = clickTrackingService.createClick(request, null);

        assertNotNull(response);
        verify(clickRepository).save(any(OutboundClick.class));
    }
}
