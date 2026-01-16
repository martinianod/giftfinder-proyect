package com.findoraai.giftfinder.giftcard.service;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.giftcard.dto.WalletLedgerEntryResponse;
import com.findoraai.giftfinder.giftcard.model.Wallet;
import com.findoraai.giftfinder.giftcard.model.WalletLedgerEntry;
import com.findoraai.giftfinder.giftcard.repository.WalletLedgerEntryRepository;
import com.findoraai.giftfinder.giftcard.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletLedgerEntryRepository ledgerRepository;

    @Override
    @Transactional
    public Wallet getOrCreateWallet(User user) {
        return walletRepository.findByUser(user)
            .orElseGet(() -> {
                Wallet wallet = Wallet.builder()
                    .user(user)
                    .balance(BigDecimal.ZERO)
                    .currency("ARS")
                    .build();
                Wallet saved = walletRepository.save(wallet);
                log.info("Created new wallet for user {}", user.getId());
                return saved;
            });
    }

    @Override
    @Transactional
    public WalletLedgerEntry credit(Wallet wallet, BigDecimal amount, WalletLedgerEntry.SourceType sourceType,
                                   String referenceId, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }

        // Update wallet balance
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        // Create ledger entry
        WalletLedgerEntry entry = WalletLedgerEntry.builder()
            .wallet(wallet)
            .amount(amount)
            .type(WalletLedgerEntry.TransactionType.CREDIT)
            .sourceType(sourceType)
            .referenceId(referenceId)
            .description(description)
            .build();
        
        WalletLedgerEntry saved = ledgerRepository.save(entry);
        log.info("Credited {} {} to wallet {}, new balance: {}", amount, wallet.getCurrency(), 
                wallet.getId(), wallet.getBalance());
        return saved;
    }

    @Override
    @Transactional
    public WalletLedgerEntry debit(Wallet wallet, BigDecimal amount, WalletLedgerEntry.SourceType sourceType,
                                  String referenceId, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        // Update wallet balance
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        // Create ledger entry
        WalletLedgerEntry entry = WalletLedgerEntry.builder()
            .wallet(wallet)
            .amount(amount)
            .type(WalletLedgerEntry.TransactionType.DEBIT)
            .sourceType(sourceType)
            .referenceId(referenceId)
            .description(description)
            .build();
        
        WalletLedgerEntry saved = ledgerRepository.save(entry);
        log.info("Debited {} {} from wallet {}, new balance: {}", amount, wallet.getCurrency(), 
                wallet.getId(), wallet.getBalance());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(User user) {
        return walletRepository.findByUser(user)
            .map(Wallet::getBalance)
            .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletLedgerEntryResponse> getLedgerEntries(Wallet wallet) {
        return ledgerRepository.findByWalletOrderByCreatedAtDesc(wallet)
            .stream()
            .map(WalletLedgerEntryResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean transactionExists(String referenceId, WalletLedgerEntry.SourceType sourceType) {
        return ledgerRepository.findByReferenceIdAndSourceType(referenceId, sourceType).isPresent();
    }
}
