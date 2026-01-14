package com.findoraai.giftfinder.giftcard.service;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.giftcard.dto.WalletLedgerEntryResponse;
import com.findoraai.giftfinder.giftcard.model.Wallet;
import com.findoraai.giftfinder.giftcard.model.WalletLedgerEntry;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {
    /**
     * Get or create a wallet for a user
     * @param user The user
     * @return The wallet
     */
    Wallet getOrCreateWallet(User user);

    /**
     * Credit an amount to a wallet
     * @param wallet The wallet
     * @param amount The amount to credit
     * @param sourceType The source type
     * @param referenceId The reference ID (e.g., gift card code)
     * @param description Optional description
     * @return The ledger entry
     */
    WalletLedgerEntry credit(Wallet wallet, BigDecimal amount, WalletLedgerEntry.SourceType sourceType, 
                            String referenceId, String description);

    /**
     * Debit an amount from a wallet
     * @param wallet The wallet
     * @param amount The amount to debit
     * @param sourceType The source type
     * @param referenceId The reference ID
     * @param description Optional description
     * @return The ledger entry
     */
    WalletLedgerEntry debit(Wallet wallet, BigDecimal amount, WalletLedgerEntry.SourceType sourceType, 
                           String referenceId, String description);

    /**
     * Get wallet balance for a user
     * @param user The user
     * @return The balance
     */
    BigDecimal getBalance(User user);

    /**
     * Get ledger entries for a wallet
     * @param wallet The wallet
     * @return List of ledger entries
     */
    List<WalletLedgerEntryResponse> getLedgerEntries(Wallet wallet);

    /**
     * Check if a transaction already exists (for idempotency)
     * @param referenceId The reference ID
     * @param sourceType The source type
     * @return true if transaction exists
     */
    boolean transactionExists(String referenceId, WalletLedgerEntry.SourceType sourceType);
}
