package com.findoraai.giftfinder.giftcard.service;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.giftcard.model.Wallet;
import com.findoraai.giftfinder.giftcard.model.WalletLedgerEntry;
import com.findoraai.giftfinder.giftcard.repository.WalletLedgerEntryRepository;
import com.findoraai.giftfinder.giftcard.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletLedgerEntryRepository ledgerRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    private User testUser;
    private Wallet testWallet;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .email("test@example.com")
            .name("Test User")
            .build();

        testWallet = Wallet.builder()
            .id(1L)
            .user(testUser)
            .balance(BigDecimal.valueOf(100))
            .currency("ARS")
            .build();
    }

    @Test
    void testGetOrCreateWallet_ExistingWallet() {
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(testWallet));

        Wallet result = walletService.getOrCreateWallet(testUser);

        assertNotNull(result);
        assertEquals(testWallet.getId(), result.getId());
        verify(walletRepository).findByUser(testUser);
        verify(walletRepository, never()).save(any());
    }

    @Test
    void testGetOrCreateWallet_NewWallet() {
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        Wallet result = walletService.getOrCreateWallet(testUser);

        assertNotNull(result);
        verify(walletRepository).findByUser(testUser);
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void testCredit_Success() {
        BigDecimal amount = BigDecimal.valueOf(50);
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
        
        WalletLedgerEntry entry = WalletLedgerEntry.builder()
            .id(1L)
            .wallet(testWallet)
            .amount(amount)
            .type(WalletLedgerEntry.TransactionType.CREDIT)
            .sourceType(WalletLedgerEntry.SourceType.GIFT_CARD_REDEMPTION)
            .referenceId("TEST123")
            .build();
        
        when(ledgerRepository.save(any(WalletLedgerEntry.class))).thenReturn(entry);

        WalletLedgerEntry result = walletService.credit(
            testWallet,
            amount,
            WalletLedgerEntry.SourceType.GIFT_CARD_REDEMPTION,
            "TEST123",
            "Test credit"
        );

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(150), testWallet.getBalance());
        verify(walletRepository).save(testWallet);
        verify(ledgerRepository).save(any(WalletLedgerEntry.class));
    }

    @Test
    void testCredit_NegativeAmount() {
        BigDecimal amount = BigDecimal.valueOf(-50);

        assertThrows(IllegalArgumentException.class, () ->
            walletService.credit(
                testWallet,
                amount,
                WalletLedgerEntry.SourceType.GIFT_CARD_REDEMPTION,
                "TEST123",
                "Test credit"
            )
        );

        verify(walletRepository, never()).save(any());
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    void testDebit_Success() {
        BigDecimal amount = BigDecimal.valueOf(50);
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
        
        WalletLedgerEntry entry = WalletLedgerEntry.builder()
            .id(1L)
            .wallet(testWallet)
            .amount(amount)
            .type(WalletLedgerEntry.TransactionType.DEBIT)
            .sourceType(WalletLedgerEntry.SourceType.PURCHASE)
            .referenceId("PURCHASE123")
            .build();
        
        when(ledgerRepository.save(any(WalletLedgerEntry.class))).thenReturn(entry);

        WalletLedgerEntry result = walletService.debit(
            testWallet,
            amount,
            WalletLedgerEntry.SourceType.PURCHASE,
            "PURCHASE123",
            "Test debit"
        );

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(50), testWallet.getBalance());
        verify(walletRepository).save(testWallet);
        verify(ledgerRepository).save(any(WalletLedgerEntry.class));
    }

    @Test
    void testDebit_InsufficientBalance() {
        BigDecimal amount = BigDecimal.valueOf(150);

        assertThrows(IllegalArgumentException.class, () ->
            walletService.debit(
                testWallet,
                amount,
                WalletLedgerEntry.SourceType.PURCHASE,
                "PURCHASE123",
                "Test debit"
            )
        );

        verify(walletRepository, never()).save(any());
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    void testGetBalance_ExistingWallet() {
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.of(testWallet));

        BigDecimal balance = walletService.getBalance(testUser);

        assertEquals(BigDecimal.valueOf(100), balance);
        verify(walletRepository).findByUser(testUser);
    }

    @Test
    void testGetBalance_NoWallet() {
        when(walletRepository.findByUser(testUser)).thenReturn(Optional.empty());

        BigDecimal balance = walletService.getBalance(testUser);

        assertEquals(BigDecimal.ZERO, balance);
        verify(walletRepository).findByUser(testUser);
    }

    @Test
    void testTransactionExists_True() {
        when(ledgerRepository.findByReferenceIdAndSourceType(
            "TEST123",
            WalletLedgerEntry.SourceType.GIFT_CARD_REDEMPTION
        )).thenReturn(Optional.of(new WalletLedgerEntry()));

        boolean exists = walletService.transactionExists(
            "TEST123",
            WalletLedgerEntry.SourceType.GIFT_CARD_REDEMPTION
        );

        assertTrue(exists);
    }

    @Test
    void testTransactionExists_False() {
        when(ledgerRepository.findByReferenceIdAndSourceType(
            "TEST123",
            WalletLedgerEntry.SourceType.GIFT_CARD_REDEMPTION
        )).thenReturn(Optional.empty());

        boolean exists = walletService.transactionExists(
            "TEST123",
            WalletLedgerEntry.SourceType.GIFT_CARD_REDEMPTION
        );

        assertFalse(exists);
    }
}
