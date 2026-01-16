package com.findoraai.giftfinder.profile.service;

import com.findoraai.giftfinder.auth.model.Role;
import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.profile.dto.PublicProfileResponse;
import com.findoraai.giftfinder.profile.model.RecipientProfile;
import com.findoraai.giftfinder.profile.model.ShareLinkToken;
import com.findoraai.giftfinder.profile.repository.ShareLinkTokenRepository;
import com.findoraai.giftfinder.scraper.dto.ScraperResponse;
import com.findoraai.giftfinder.scraper.service.ScraperService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicProfileServiceImplTest {

    @Mock
    private ShareLinkTokenRepository tokenRepository;

    @Mock
    private ScraperService scraperService;

    @InjectMocks
    private PublicProfileServiceImpl publicProfileService;

    private User testUser;
    private RecipientProfile testProfile;
    private ShareLinkToken testToken;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .name("Test User")
                .role(Role.USER)
                .build();

        testProfile = RecipientProfile.builder()
                .id(1L)
                .user(testUser)
                .name("John Doe")
                .relationship("friend")
                .interests("technology, gaming")
                .restrictions("no food items")
                .sizes("M")
                .preferredStores("Amazon")
                .budgetMin(BigDecimal.valueOf(50))
                .budgetMax(BigDecimal.valueOf(200))
                .visibility(RecipientProfile.ProfileVisibility.SHARED_LINK)
                .claimed(false)
                .wishlistItems(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testToken = ShareLinkToken.builder()
                .id(1L)
                .profile(testProfile)
                .hashedToken("valid-token-123")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(1))
                .build();

        testProfile.setShareToken(testToken);
    }

    @Test
    void testGetPublicProfile_Success() {
        when(tokenRepository.findByHashedToken("valid-token-123")).thenReturn(Optional.of(testToken));

        PublicProfileResponse result = publicProfileService.getPublicProfile("valid-token-123");

        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals("friend", result.getRelationship());
        assertEquals("technology, gaming", result.getInterests());
        assertEquals("no food items", result.getRestrictions());
        assertEquals("M", result.getSizes());
        assertEquals("Amazon", result.getPreferredStores());
        assertEquals(BigDecimal.valueOf(50), result.getBudgetMin());
        assertEquals(BigDecimal.valueOf(200), result.getBudgetMax());
        assertFalse(result.isClaimed());
        assertNotNull(result.getWishlistItems());
        verify(tokenRepository).findByHashedToken("valid-token-123");
    }

    @Test
    void testGetPublicProfile_InvalidToken() {
        when(tokenRepository.findByHashedToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> publicProfileService.getPublicProfile("invalid-token"));
        verify(tokenRepository).findByHashedToken("invalid-token");
    }

    @Test
    void testGetPublicProfile_ExpiredToken() {
        testToken.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(tokenRepository.findByHashedToken("expired-token")).thenReturn(Optional.of(testToken));

        assertThrows(RuntimeException.class, () -> publicProfileService.getPublicProfile("expired-token"));
        verify(tokenRepository).findByHashedToken("expired-token");
    }

    @Test
    void testGetPublicProfile_PrivateProfile() {
        testProfile.setVisibility(RecipientProfile.ProfileVisibility.PRIVATE);
        when(tokenRepository.findByHashedToken("valid-token-123")).thenReturn(Optional.of(testToken));

        assertThrows(RuntimeException.class, () -> publicProfileService.getPublicProfile("valid-token-123"));
        verify(tokenRepository).findByHashedToken("valid-token-123");
    }

    @Test
    void testGetGiftRecommendations_Success() {
        ScraperResponse mockResponse = new ScraperResponse();
        
        when(tokenRepository.findByHashedToken("valid-token-123")).thenReturn(Optional.of(testToken));
        when(scraperService.search(anyString())).thenReturn(mockResponse);

        ScraperResponse result = publicProfileService.getGiftRecommendations("valid-token-123");

        assertNotNull(result);
        verify(tokenRepository).findByHashedToken("valid-token-123");
        verify(scraperService).search(anyString());
    }

    @Test
    void testGetGiftRecommendations_InvalidToken() {
        when(tokenRepository.findByHashedToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> publicProfileService.getGiftRecommendations("invalid-token"));
        verify(tokenRepository).findByHashedToken("invalid-token");
        verify(scraperService, never()).search(anyString());
    }

    @Test
    void testGetGiftRecommendations_ExpiredToken() {
        testToken.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(tokenRepository.findByHashedToken("expired-token")).thenReturn(Optional.of(testToken));

        assertThrows(RuntimeException.class, () -> publicProfileService.getGiftRecommendations("expired-token"));
        verify(tokenRepository).findByHashedToken("expired-token");
        verify(scraperService, never()).search(anyString());
    }

    @Test
    void testGetGiftRecommendations_BuildsQueryFromProfile() {
        when(tokenRepository.findByHashedToken("valid-token-123")).thenReturn(Optional.of(testToken));
        when(scraperService.search(anyString())).thenReturn(new ScraperResponse());

        publicProfileService.getGiftRecommendations("valid-token-123");

        verify(scraperService).search(argThat(query -> 
            query.contains("technology, gaming") && query.contains("friend")
        ));
    }
}
