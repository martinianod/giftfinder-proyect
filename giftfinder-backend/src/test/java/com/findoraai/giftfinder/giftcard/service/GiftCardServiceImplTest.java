package com.findoraai.giftfinder.giftcard.service;

import com.findoraai.giftfinder.auth.model.Role;
import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.giftcard.dto.CreateGiftCardRequest;
import com.findoraai.giftfinder.giftcard.dto.GiftCardResponse;
import com.findoraai.giftfinder.giftcard.model.GiftCard;
import com.findoraai.giftfinder.giftcard.model.Wallet;
import com.findoraai.giftfinder.giftcard.model.WalletLedgerEntry;
import com.findoraai.giftfinder.giftcard.repository.GiftCardRepository;
import com.findoraai.giftfinder.notifications.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GiftCardServiceImplTest {

    @Mock
    private GiftCardRepository giftCardRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private GiftCardServiceImpl giftCardService;

    private User sender;
    private User recipient;
    private GiftCard testGiftCard;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(giftCardService, "appBaseUrl", "http://localhost:5173");

        sender = User.builder()
            .id(1L)
            .email("sender@example.com")
            .name("Sender User")
            .role(Role.USER)
            .build();

        recipient = User.builder()
            .id(2L)
            .email("recipient@example.com")
            .name("Recipient User")
            .role(Role.USER)
            .build();

        testGiftCard = GiftCard.builder()
            .id(1L)
            .code("ABC123XYZ456")
            .hashedCode("$2a$10$hashedcode")
            .amount(BigDecimal.valueOf(100))
            .currency("ARS")
            .message("Happy Birthday!")
            .sender(sender)
            .recipientEmail("recipient@example.com")
            .status(GiftCard.GiftCardStatus.CREATED)
            .expiryDate(LocalDate.now().plusMonths(12))
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    @Test
    void testCreateGiftCard_Success() {
        CreateGiftCardRequest request = CreateGiftCardRequest.builder()
            .amount(BigDecimal.valueOf(100))
            .currency("ARS")
            .message("Happy Birthday!")
            .recipientEmail("recipient@example.com")
            .deliveryDate(LocalDate.now().plusDays(1))
            .expiryMonths(12)
            .build();

        when(giftCardRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedcode");
        when(giftCardRepository.save(any(GiftCard.class))).thenReturn(testGiftCard);

        GiftCardResponse response = giftCardService.createGiftCard(request, sender);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(100), response.getAmount());
        assertEquals("ARS", response.getCurrency());
        assertEquals("recipient@example.com", response.getRecipientEmail());
        verify(giftCardRepository).save(any(GiftCard.class));
        // Should not send immediately since delivery date is in the future
        verify(notificationService, never()).sendEmail(anyString(), anyString(), anyString(), any());
    }

    @Test
    void testSendGiftCard_Success() {
        when(giftCardRepository.findByCode("ABC123XYZ456")).thenReturn(Optional.of(testGiftCard));
        when(giftCardRepository.save(any(GiftCard.class))).thenReturn(testGiftCard);
        when(notificationService.sendEmail(anyString(), anyString(), anyString(), any())).thenReturn(true);

        giftCardService.sendGiftCard("ABC123XYZ456");

        assertEquals(GiftCard.GiftCardStatus.SENT, testGiftCard.getStatus());
        verify(giftCardRepository).save(testGiftCard);
        verify(notificationService).sendEmail(
            eq("recipient@example.com"),
            anyString(),
            eq("gift_card"),
            any()
        );
    }

    @Test
    void testSendGiftCard_AlreadySent() {
        testGiftCard.setStatus(GiftCard.GiftCardStatus.SENT);
        when(giftCardRepository.findByCode("ABC123XYZ456")).thenReturn(Optional.of(testGiftCard));

        giftCardService.sendGiftCard("ABC123XYZ456");

        verify(giftCardRepository, never()).save(any());
        verify(notificationService, never()).sendEmail(anyString(), anyString(), anyString(), any());
    }

    @Test
    void testRedeemGiftCard_Success() {
        testGiftCard.setStatus(GiftCard.GiftCardStatus.SENT);
        Wallet wallet = Wallet.builder()
            .id(1L)
            .user(recipient)
            .balance(BigDecimal.ZERO)
            .currency("ARS")
            .build();

        when(giftCardRepository.findByCode("ABC123XYZ456")).thenReturn(Optional.of(testGiftCard));
        when(walletService.transactionExists(anyString(), any())).thenReturn(false);
        when(walletService.getOrCreateWallet(recipient)).thenReturn(wallet);
        when(giftCardRepository.save(any(GiftCard.class))).thenReturn(testGiftCard);

        GiftCardResponse response = giftCardService.redeemGiftCard("ABC123XYZ456", recipient);

        assertNotNull(response);
        assertEquals(GiftCard.GiftCardStatus.REDEEMED, testGiftCard.getStatus());
        assertNotNull(testGiftCard.getRedeemedAt());
        verify(walletService).credit(
            eq(wallet),
            eq(BigDecimal.valueOf(100)),
            eq(WalletLedgerEntry.SourceType.GIFT_CARD_REDEMPTION),
            eq("ABC123XYZ456"),
            anyString()
        );
        verify(giftCardRepository).save(testGiftCard);
    }

    @Test
    void testRedeemGiftCard_AlreadyRedeemed() {
        testGiftCard.setStatus(GiftCard.GiftCardStatus.REDEEMED);
        when(giftCardRepository.findByCode("ABC123XYZ456")).thenReturn(Optional.of(testGiftCard));

        assertThrows(IllegalStateException.class, () ->
            giftCardService.redeemGiftCard("ABC123XYZ456", recipient)
        );

        verify(walletService, never()).credit(any(), any(), any(), anyString(), anyString());
        verify(giftCardRepository, never()).save(any());
    }

    @Test
    void testRedeemGiftCard_Cancelled() {
        testGiftCard.setStatus(GiftCard.GiftCardStatus.CANCELLED);
        when(giftCardRepository.findByCode("ABC123XYZ456")).thenReturn(Optional.of(testGiftCard));

        assertThrows(IllegalStateException.class, () ->
            giftCardService.redeemGiftCard("ABC123XYZ456", recipient)
        );

        verify(walletService, never()).credit(any(), any(), any(), anyString(), anyString());
    }

    @Test
    void testRedeemGiftCard_Expired() {
        testGiftCard.setExpiryDate(LocalDate.now().minusDays(1));
        testGiftCard.setStatus(GiftCard.GiftCardStatus.SENT);
        when(giftCardRepository.findByCode("ABC123XYZ456")).thenReturn(Optional.of(testGiftCard));
        when(giftCardRepository.save(any(GiftCard.class))).thenReturn(testGiftCard);

        assertThrows(IllegalStateException.class, () ->
            giftCardService.redeemGiftCard("ABC123XYZ456", recipient)
        );

        assertEquals(GiftCard.GiftCardStatus.EXPIRED, testGiftCard.getStatus());
        verify(giftCardRepository).save(testGiftCard);
        verify(walletService, never()).credit(any(), any(), any(), anyString(), anyString());
    }

    @Test
    void testRedeemGiftCard_Idempotency() {
        testGiftCard.setStatus(GiftCard.GiftCardStatus.SENT);
        when(giftCardRepository.findByCode("ABC123XYZ456")).thenReturn(Optional.of(testGiftCard));
        when(walletService.transactionExists("ABC123XYZ456", WalletLedgerEntry.SourceType.GIFT_CARD_REDEMPTION))
            .thenReturn(true);

        GiftCardResponse response = giftCardService.redeemGiftCard("ABC123XYZ456", recipient);

        assertNotNull(response);
        verify(walletService, never()).credit(any(), any(), any(), anyString(), anyString());
        verify(giftCardRepository, never()).save(any());
    }

    @Test
    void testCancelGiftCard_Success() {
        when(giftCardRepository.findById(1L)).thenReturn(Optional.of(testGiftCard));
        when(giftCardRepository.save(any(GiftCard.class))).thenReturn(testGiftCard);

        giftCardService.cancelGiftCard(1L);

        assertEquals(GiftCard.GiftCardStatus.CANCELLED, testGiftCard.getStatus());
        verify(giftCardRepository).save(testGiftCard);
    }

    @Test
    void testCancelGiftCard_AlreadyRedeemed() {
        testGiftCard.setStatus(GiftCard.GiftCardStatus.REDEEMED);
        when(giftCardRepository.findById(1L)).thenReturn(Optional.of(testGiftCard));

        assertThrows(IllegalStateException.class, () ->
            giftCardService.cancelGiftCard(1L)
        );

        verify(giftCardRepository, never()).save(any());
    }

    @Test
    void testExpireGiftCard_Success() {
        when(giftCardRepository.findById(1L)).thenReturn(Optional.of(testGiftCard));
        when(giftCardRepository.save(any(GiftCard.class))).thenReturn(testGiftCard);

        giftCardService.expireGiftCard(1L);

        assertEquals(GiftCard.GiftCardStatus.EXPIRED, testGiftCard.getStatus());
        verify(giftCardRepository).save(testGiftCard);
    }

    @Test
    void testGetGiftCardsBySender() {
        List<GiftCard> giftCards = List.of(testGiftCard);
        when(giftCardRepository.findBySenderOrderByCreatedAtDesc(sender)).thenReturn(giftCards);

        List<GiftCardResponse> responses = giftCardService.getGiftCardsBySender(sender);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        verify(giftCardRepository).findBySenderOrderByCreatedAtDesc(sender);
    }

    @Test
    void testProcessExpiredGiftCards() {
        GiftCard expiredCard = GiftCard.builder()
            .id(2L)
            .code("EXPIRED123")
            .hashedCode("$2a$10$hashedcode")
            .amount(BigDecimal.valueOf(50))
            .currency("ARS")
            .sender(sender)
            .recipientEmail("test@example.com")
            .status(GiftCard.GiftCardStatus.SENT)
            .expiryDate(LocalDate.now().minusDays(1))
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        when(giftCardRepository.findAll()).thenReturn(List.of(testGiftCard, expiredCard));
        when(giftCardRepository.save(any(GiftCard.class))).thenReturn(expiredCard);

        giftCardService.processExpiredGiftCards();

        verify(giftCardRepository).save(expiredCard);
        assertEquals(GiftCard.GiftCardStatus.EXPIRED, expiredCard.getStatus());
    }
}
