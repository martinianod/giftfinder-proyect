package com.findoraai.giftfinder.scheduler;

import com.findoraai.giftfinder.giftcard.service.GiftCardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GiftCardExpirationJob {

    private final GiftCardService giftCardService;

    /**
     * Process expired gift cards daily at 3 AM
     */
    @Scheduled(cron = "${scheduler.gift-card-expiration.cron:0 0 3 * * *}")
    public void processExpiredGiftCards() {
        log.info("Starting gift card expiration job");
        try {
            giftCardService.processExpiredGiftCards();
            log.info("Gift card expiration job completed successfully");
        } catch (Exception e) {
            log.error("Error processing expired gift cards", e);
        }
    }
}
