package com.findoraai.giftfinder.giftcard.dto;

import com.findoraai.giftfinder.giftcard.model.Wallet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private Long id;
    private BigDecimal balance;
    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<WalletLedgerEntryResponse> recentTransactions;
    
    public static WalletResponse fromEntity(Wallet wallet, List<WalletLedgerEntryResponse> recentTransactions) {
        return WalletResponse.builder()
            .id(wallet.getId())
            .balance(wallet.getBalance())
            .currency(wallet.getCurrency())
            .createdAt(wallet.getCreatedAt())
            .updatedAt(wallet.getUpdatedAt())
            .recentTransactions(recentTransactions)
            .build();
    }
}
