package com.findoraai.giftfinder.giftcard.service;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.giftcard.dto.CreateGiftCardRequest;
import com.findoraai.giftfinder.giftcard.dto.GiftCardResponse;
import com.findoraai.giftfinder.giftcard.model.GiftCard;
import com.findoraai.giftfinder.giftcard.model.Wallet;
import com.findoraai.giftfinder.giftcard.model.WalletLedgerEntry;
import com.findoraai.giftfinder.giftcard.repository.GiftCardRepository;
import com.findoraai.giftfinder.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GiftCardServiceImpl implements GiftCardService {

    private final GiftCardRepository giftCardRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.base-url:http://localhost:5173}")
    private String appBaseUrl;

    @Override
    @Transactional
    public GiftCardResponse createGiftCard(CreateGiftCardRequest request, User sender) {
        // Generate unique code
        String code = generateUniqueCode();
        String hashedCode = passwordEncoder.encode(code);

        // Calculate expiry date
        LocalDate expiryDate = request.getExpiryMonths() != null
            ? LocalDate.now().plusMonths(request.getExpiryMonths())
            : LocalDate.now().plusMonths(12);

        // Create gift card
        GiftCard giftCard = GiftCard.builder()
            .code(code)
            .hashedCode(hashedCode)
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .message(request.getMessage())
            .sender(sender)
            .recipientEmail(request.getRecipientEmail())
            .deliveryDate(request.getDeliveryDate())
            .status(GiftCard.GiftCardStatus.CREATED)
            .expiryDate(expiryDate)
            .build();

        GiftCard saved = giftCardRepository.save(giftCard);
        log.info("Created gift card {} for {} from user {}", saved.getId(), 
                request.getRecipientEmail(), sender.getId());

        // If delivery date is null or today, send immediately
        if (request.getDeliveryDate() == null || !request.getDeliveryDate().isAfter(LocalDate.now())) {
            sendGiftCard(code);
        }

        return GiftCardResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void sendGiftCard(String code) {
        GiftCard giftCard = giftCardRepository.findByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Gift card not found"));

        if (giftCard.getStatus() != GiftCard.GiftCardStatus.CREATED) {
            log.warn("Gift card {} already sent or invalid status: {}", giftCard.getId(), giftCard.getStatus());
            return;
        }

        // Update status to SENT
        giftCard.setStatus(GiftCard.GiftCardStatus.SENT);
        giftCardRepository.save(giftCard);

        // Send email notification
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("senderName", giftCard.getSender().getName());
        templateData.put("amount", giftCard.getAmount());
        templateData.put("currency", giftCard.getCurrency());
        templateData.put("message", giftCard.getMessage());
        templateData.put("code", giftCard.getCode());
        templateData.put("expiryDate", giftCard.getExpiryDate().toString());
        templateData.put("appUrl", appBaseUrl);

        boolean sent = notificationService.sendEmail(
            giftCard.getRecipientEmail(),
            "You've received a gift card!",
            "gift_card",
            templateData
        );

        if (sent) {
            log.info("Sent gift card {} to {}", giftCard.getId(), giftCard.getRecipientEmail());
        } else {
            log.error("Failed to send gift card {} to {}", giftCard.getId(), giftCard.getRecipientEmail());
        }
    }

    @Override
    @Transactional
    public GiftCardResponse redeemGiftCard(String code, User redeemer) {
        GiftCard giftCard = giftCardRepository.findByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Invalid gift card code"));

        // Validate gift card status
        if (giftCard.getStatus() == GiftCard.GiftCardStatus.REDEEMED) {
            throw new IllegalStateException("Gift card has already been redeemed");
        }
        if (giftCard.getStatus() == GiftCard.GiftCardStatus.CANCELLED) {
            throw new IllegalStateException("Gift card has been cancelled");
        }
        if (giftCard.getStatus() == GiftCard.GiftCardStatus.EXPIRED) {
            throw new IllegalStateException("Gift card has expired");
        }

        // Check expiry date
        if (LocalDate.now().isAfter(giftCard.getExpiryDate())) {
            giftCard.setStatus(GiftCard.GiftCardStatus.EXPIRED);
            giftCardRepository.save(giftCard);
            throw new IllegalStateException("Gift card has expired");
        }

        // Check idempotency - if already processed in ledger, don't process again
        if (walletService.transactionExists(code, WalletLedgerEntry.SourceType.GIFT_CARD_REDEMPTION)) {
            log.warn("Gift card {} redemption already processed", code);
            return GiftCardResponse.fromEntity(giftCard);
        }

        // Get or create wallet
        Wallet wallet = walletService.getOrCreateWallet(redeemer);

        // Credit wallet
        walletService.credit(
            wallet,
            giftCard.getAmount(),
            WalletLedgerEntry.SourceType.GIFT_CARD_REDEMPTION,
            code,
            "Gift card redemption: " + giftCard.getMessage()
        );

        // Update gift card status
        giftCard.setStatus(GiftCard.GiftCardStatus.REDEEMED);
        giftCard.setRedeemedAt(LocalDateTime.now());
        giftCard.setRedeemedBy(redeemer);
        GiftCard saved = giftCardRepository.save(giftCard);

        log.info("Redeemed gift card {} by user {}", giftCard.getId(), redeemer.getId());

        return GiftCardResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void cancelGiftCard(Long id) {
        GiftCard giftCard = giftCardRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Gift card not found"));

        if (giftCard.getStatus() == GiftCard.GiftCardStatus.REDEEMED) {
            throw new IllegalStateException("Cannot cancel a redeemed gift card");
        }

        giftCard.setStatus(GiftCard.GiftCardStatus.CANCELLED);
        giftCardRepository.save(giftCard);
        log.info("Cancelled gift card {}", id);
    }

    @Override
    @Transactional
    public void expireGiftCard(Long id) {
        GiftCard giftCard = giftCardRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Gift card not found"));

        if (giftCard.getStatus() == GiftCard.GiftCardStatus.REDEEMED) {
            throw new IllegalStateException("Cannot expire a redeemed gift card");
        }

        giftCard.setStatus(GiftCard.GiftCardStatus.EXPIRED);
        giftCardRepository.save(giftCard);
        log.info("Expired gift card {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GiftCardResponse> getGiftCardsBySender(User user) {
        return giftCardRepository.findBySenderOrderByCreatedAtDesc(user)
            .stream()
            .map(GiftCardResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public GiftCard getByCode(String code) {
        return giftCardRepository.findByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Gift card not found"));
    }

    @Override
    @Transactional
    public void processExpiredGiftCards() {
        List<GiftCard> expiredCards = giftCardRepository.findAll().stream()
            .filter(gc -> gc.getStatus() != GiftCard.GiftCardStatus.REDEEMED
                       && gc.getStatus() != GiftCard.GiftCardStatus.CANCELLED
                       && gc.getStatus() != GiftCard.GiftCardStatus.EXPIRED
                       && LocalDate.now().isAfter(gc.getExpiryDate()))
            .collect(Collectors.toList());

        for (GiftCard giftCard : expiredCards) {
            giftCard.setStatus(GiftCard.GiftCardStatus.EXPIRED);
            giftCardRepository.save(giftCard);
            log.info("Expired gift card {}", giftCard.getId());
        }

        log.info("Processed {} expired gift cards", expiredCards.size());
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        } while (giftCardRepository.findByCode(code).isPresent());
        return code;
    }
}
