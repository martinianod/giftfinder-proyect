package com.findoraai.giftfinder.giftcard.dto;

import com.findoraai.giftfinder.giftcard.model.WalletLedgerEntry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletLedgerEntryResponse {
    private Long id;
    private BigDecimal amount;
    private WalletLedgerEntry.TransactionType type;
    private WalletLedgerEntry.SourceType sourceType;
    private String referenceId;
    private String description;
    private LocalDateTime createdAt;
    
    public static WalletLedgerEntryResponse fromEntity(WalletLedgerEntry entry) {
        return WalletLedgerEntryResponse.builder()
            .id(entry.getId())
            .amount(entry.getAmount())
            .type(entry.getType())
            .sourceType(entry.getSourceType())
            .referenceId(entry.getReferenceId())
            .description(entry.getDescription())
            .createdAt(entry.getCreatedAt())
            .build();
    }
}
