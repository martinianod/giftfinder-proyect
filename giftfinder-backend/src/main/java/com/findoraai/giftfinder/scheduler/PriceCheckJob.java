package com.findoraai.giftfinder.scheduler;

import com.findoraai.giftfinder.notifications.model.NotificationLog;
import com.findoraai.giftfinder.notifications.model.NotificationPreferences;
import com.findoraai.giftfinder.notifications.model.PriceHistory;
import com.findoraai.giftfinder.notifications.model.Reminder;
import com.findoraai.giftfinder.notifications.model.SavedProduct;
import com.findoraai.giftfinder.notifications.repository.NotificationPreferencesRepository;
import com.findoraai.giftfinder.notifications.repository.PriceHistoryRepository;
import com.findoraai.giftfinder.notifications.repository.SavedProductRepository;
import com.findoraai.giftfinder.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceCheckJob {

    private final SavedProductRepository savedProductRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final NotificationPreferencesRepository preferencesRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "${scheduler.price-check.cron:0 0 */12 * * *}")
    @Transactional
    public void checkPrices() {
        log.info("Starting price check job");
        long startTime = System.currentTimeMillis();
        int checkedCount = 0;
        int alertsSent = 0;

        try {
            // Get all products with price tracking enabled
            List<SavedProduct> trackedProducts = savedProductRepository.findByPriceTrackingEnabled(true);
            log.info("Found {} products with price tracking enabled", trackedProducts.size());

            for (SavedProduct product : trackedProducts) {
                try {
                    // Check user's notification preferences
                    NotificationPreferences prefs = preferencesRepository.findByUser(product.getUser())
                        .orElse(null);
                    
                    if (prefs == null || !prefs.getPriceDropAlertsEnabled()) {
                        continue;
                    }

                    // Get the latest price from history
                    Optional<PriceHistory> latestHistoryOpt = priceHistoryRepository.findLatestByProduct(product);
                    
                    if (latestHistoryOpt.isEmpty()) {
                        // Create initial price history entry
                        PriceHistory history = PriceHistory.builder()
                            .savedProduct(product)
                            .price(product.getCurrentPrice())
                            .currency(product.getCurrency())
                            .available(true)
                            .build();
                        priceHistoryRepository.save(history);
                        checkedCount++;
                        continue;
                    }

                    PriceHistory latestHistory = latestHistoryOpt.get();
                    BigDecimal oldPrice = latestHistory.getPrice();
                    BigDecimal currentPrice = product.getCurrentPrice();

                    // Calculate price drop percentage
                    BigDecimal priceDifference = oldPrice.subtract(currentPrice);
                    BigDecimal dropPercentage = priceDifference
                        .divide(oldPrice, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));

                    // Check if price drop threshold is met
                    if (dropPercentage.compareTo(product.getPriceDropThresholdPercent()) >= 0) {
                        // Check for duplicate notification
                        String referenceId = String.format("price-drop-%s", product.getProductId());
                        
                        if (!notificationService.wasRecentlySent(
                                product.getUser(),
                                NotificationLog.NotificationType.PRICE_DROP,
                                referenceId,
                                Reminder.NotificationChannel.EMAIL)) {
                            
                            // Send price drop notification
                            boolean sent = notificationService.sendPriceDropNotification(
                                product.getUser(),
                                product.getTitle(),
                                product.getProductUrl(),
                                product.getImageUrl(),
                                oldPrice.setScale(2, RoundingMode.HALF_UP).toString(),
                                currentPrice.setScale(2, RoundingMode.HALF_UP).toString(),
                                product.getCurrency(),
                                dropPercentage.setScale(2, RoundingMode.HALF_UP).toString(),
                                priceDifference.setScale(2, RoundingMode.HALF_UP).toString()
                            );

                            if (sent) {
                                alertsSent++;
                            }
                        }
                    }

                    // TODO: In production, integrate with scraper service to fetch actual current prices
                    // For now, we just record the current price as history
                    // Example: BigDecimal realCurrentPrice = scraperClient.fetchProductPrice(product.getProductId());
                    
                    // Create new price history entry
                    PriceHistory newHistory = PriceHistory.builder()
                        .savedProduct(product)
                        .price(currentPrice)
                        .currency(product.getCurrency())
                        .available(true)
                        .build();
                    priceHistoryRepository.save(newHistory);
                    
                    checkedCount++;

                } catch (Exception e) {
                    log.error("Error checking price for product {}: {}", product.getId(), e.getMessage());
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Price check job completed. Checked: {}, Alerts sent: {}, Duration: {}ms", 
                checkedCount, alertsSent, duration);

        } catch (Exception e) {
            log.error("Error during price check job: {}", e.getMessage(), e);
        }
    }
}
