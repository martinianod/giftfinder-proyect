package com.findoraai.giftfinder.giftcard.controller;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.giftcard.dto.WalletResponse;
import com.findoraai.giftfinder.giftcard.model.Wallet;
import com.findoraai.giftfinder.giftcard.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    /**
     * Get wallet and recent transactions for the current user
     * GET /api/wallet
     */
    @GetMapping
    public ResponseEntity<WalletResponse> getWallet(@AuthenticationPrincipal User user) {
        Wallet wallet = walletService.getOrCreateWallet(user);
        WalletResponse response = WalletResponse.fromEntity(
            wallet,
            walletService.getLedgerEntries(wallet)
        );
        return ResponseEntity.ok(response);
    }
}
