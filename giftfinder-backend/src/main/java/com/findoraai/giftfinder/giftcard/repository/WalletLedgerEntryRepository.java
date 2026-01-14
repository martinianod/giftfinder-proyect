package com.findoraai.giftfinder.giftcard.repository;

import com.findoraai.giftfinder.giftcard.model.Wallet;
import com.findoraai.giftfinder.giftcard.model.WalletLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletLedgerEntryRepository extends JpaRepository<WalletLedgerEntry, Long> {
    List<WalletLedgerEntry> findByWalletOrderByCreatedAtDesc(Wallet wallet);
    Optional<WalletLedgerEntry> findByReferenceIdAndSourceType(String referenceId, WalletLedgerEntry.SourceType sourceType);
}
