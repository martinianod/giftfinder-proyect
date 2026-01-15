package com.findoraai.giftfinder.profile.service;

import com.findoraai.giftfinder.auth.model.Role;
import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.profile.dto.RecipientProfileRequest;
import com.findoraai.giftfinder.profile.dto.RecipientProfileResponse;
import com.findoraai.giftfinder.profile.model.RecipientProfile;
import com.findoraai.giftfinder.profile.model.ShareLinkToken;
import com.findoraai.giftfinder.profile.repository.RecipientProfileRepository;
import com.findoraai.giftfinder.profile.repository.ShareLinkTokenRepository;
import com.findoraai.giftfinder.profile.repository.WishlistItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipientProfileServiceImplTest {

    @Mock
    private RecipientProfileRepository profileRepository;

    @Mock
    private ShareLinkTokenRepository tokenRepository;

    @Mock
    private WishlistItemRepository wishlistRepository;

    @InjectMocks
    private RecipientProfileServiceImpl profileService;

    private User testUser;
    private RecipientProfile testProfile;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(profileService, "appBaseUrl", "http://localhost:5173");

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
                .visibility(RecipientProfile.ProfileVisibility.PRIVATE)
                .claimed(false)
                .wishlistItems(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testGetUserProfiles_Success() {
        List<RecipientProfile> profiles = Arrays.asList(testProfile);
        when(profileRepository.findByUserOrderByCreatedAtDesc(testUser)).thenReturn(profiles);

        List<RecipientProfileResponse> result = profileService.getUserProfiles(testUser);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).getName());
        verify(profileRepository).findByUserOrderByCreatedAtDesc(testUser);
    }

    @Test
    void testGetProfile_Success() {
        when(profileRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testProfile));

        RecipientProfileResponse result = profileService.getProfile(1L, testUser);

        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals("friend", result.getRelationship());
        verify(profileRepository).findByIdAndUser(1L, testUser);
    }

    @Test
    void testGetProfile_NotFound() {
        when(profileRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> profileService.getProfile(1L, testUser));
        verify(profileRepository).findByIdAndUser(1L, testUser);
    }

    @Test
    void testCreateProfile_Private() {
        RecipientProfileRequest request = RecipientProfileRequest.builder()
                .name("Jane Smith")
                .relationship("sister")
                .interests("books, art")
                .visibility(RecipientProfile.ProfileVisibility.PRIVATE)
                .budgetMin(BigDecimal.valueOf(30))
                .budgetMax(BigDecimal.valueOf(100))
                .build();

        RecipientProfile savedProfile = RecipientProfile.builder()
                .id(2L)
                .user(testUser)
                .name(request.getName())
                .relationship(request.getRelationship())
                .interests(request.getInterests())
                .visibility(request.getVisibility())
                .budgetMin(request.getBudgetMin())
                .budgetMax(request.getBudgetMax())
                .claimed(false)
                .wishlistItems(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(profileRepository.save(any(RecipientProfile.class))).thenReturn(savedProfile);

        RecipientProfileResponse result = profileService.createProfile(request, testUser);

        assertNotNull(result);
        assertEquals("Jane Smith", result.getName());
        assertEquals("sister", result.getRelationship());
        assertEquals(RecipientProfile.ProfileVisibility.PRIVATE, result.getVisibility());
        assertNull(result.getShareUrl());
        verify(profileRepository).save(any(RecipientProfile.class));
    }

    @Test
    void testCreateProfile_SharedLink() {
        RecipientProfileRequest request = RecipientProfileRequest.builder()
                .name("Jane Smith")
                .relationship("sister")
                .interests("books, art")
                .visibility(RecipientProfile.ProfileVisibility.SHARED_LINK)
                .budgetMin(BigDecimal.valueOf(30))
                .budgetMax(BigDecimal.valueOf(100))
                .build();

        RecipientProfile savedProfile = RecipientProfile.builder()
                .id(2L)
                .user(testUser)
                .name(request.getName())
                .relationship(request.getRelationship())
                .interests(request.getInterests())
                .visibility(request.getVisibility())
                .budgetMin(request.getBudgetMin())
                .budgetMax(request.getBudgetMax())
                .claimed(false)
                .wishlistItems(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ShareLinkToken token = ShareLinkToken.builder()
                .id(1L)
                .profile(savedProfile)
                .hashedToken("test-token-123")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(1))
                .build();

        savedProfile.setShareToken(token);

        when(profileRepository.save(any(RecipientProfile.class))).thenReturn(savedProfile);
        when(tokenRepository.save(any(ShareLinkToken.class))).thenReturn(token);

        RecipientProfileResponse result = profileService.createProfile(request, testUser);

        assertNotNull(result);
        assertEquals("Jane Smith", result.getName());
        assertEquals(RecipientProfile.ProfileVisibility.SHARED_LINK, result.getVisibility());
        verify(profileRepository).save(any(RecipientProfile.class));
        verify(tokenRepository).save(any(ShareLinkToken.class));
    }

    @Test
    void testUpdateProfile_Success() {
        RecipientProfileRequest request = RecipientProfileRequest.builder()
                .name("John Doe Updated")
                .relationship("best friend")
                .interests("technology, gaming, photography")
                .restrictions("no food items")
                .sizes("L")
                .preferredStores("Amazon, Best Buy")
                .budgetMin(BigDecimal.valueOf(100))
                .budgetMax(BigDecimal.valueOf(300))
                .visibility(RecipientProfile.ProfileVisibility.PRIVATE)
                .build();

        when(profileRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testProfile));
        when(profileRepository.save(any(RecipientProfile.class))).thenReturn(testProfile);

        RecipientProfileResponse result = profileService.updateProfile(1L, request, testUser);

        assertNotNull(result);
        assertEquals("John Doe Updated", testProfile.getName());
        assertEquals("best friend", testProfile.getRelationship());
        verify(profileRepository).findByIdAndUser(1L, testUser);
        verify(profileRepository).save(testProfile);
    }

    @Test
    void testDeleteProfile_Success() {
        when(profileRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testProfile));
        doNothing().when(profileRepository).delete(testProfile);

        profileService.deleteProfile(1L, testUser);

        verify(profileRepository).findByIdAndUser(1L, testUser);
        verify(profileRepository).delete(testProfile);
    }

    @Test
    void testDeleteProfile_NotFound() {
        when(profileRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> profileService.deleteProfile(1L, testUser));
        verify(profileRepository).findByIdAndUser(1L, testUser);
        verify(profileRepository, never()).delete(any());
    }

    @Test
    void testGenerateShareLink_Success() {
        testProfile.setVisibility(RecipientProfile.ProfileVisibility.SHARED_LINK);
        
        ShareLinkToken token = ShareLinkToken.builder()
                .id(1L)
                .profile(testProfile)
                .hashedToken("test-token-abc123")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(1))
                .build();

        when(profileRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testProfile));
        when(tokenRepository.findByProfile(testProfile)).thenReturn(Optional.of(token));

        String result = profileService.generateShareLink(1L, testUser);

        assertNotNull(result);
        assertTrue(result.contains("test-token-abc123"));
        verify(profileRepository).findByIdAndUser(1L, testUser);
        verify(tokenRepository).findByProfile(testProfile);
    }

    @Test
    void testGenerateShareLink_PrivateProfile() {
        testProfile.setVisibility(RecipientProfile.ProfileVisibility.PRIVATE);

        when(profileRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testProfile));

        assertThrows(RuntimeException.class, () -> profileService.generateShareLink(1L, testUser));
        verify(profileRepository).findByIdAndUser(1L, testUser);
    }
}
