package com.findoraai.giftfinder.notifications.model;

import com.findoraai.giftfinder.auth.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "saved_products", indexes = {
    @Index(name = "idx_saved_product_user", columnList = "user_id"),
    @Index(name = "idx_saved_product_tracking", columnList = "price_tracking_enabled")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private Recipient recipient;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal currentPrice;

    @Column(nullable = false)
    private String currency;

    @Column(length = 2000)
    private String productUrl;

    @Column(length = 2000)
    private String imageUrl;

    private String store;

    @Column(length = 2000)
    private String affiliateUrl;

    @Column(length = 100)
    private String campaignId;

    @Column(length = 500)
    private String trackingTags;

    @Column(nullable = false)
    private Boolean priceTrackingEnabled;

    @Column(precision = 5, scale = 2)
    private BigDecimal priceDropThresholdPercent;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (priceTrackingEnabled == null) {
            priceTrackingEnabled = true;
        }
        if (priceDropThresholdPercent == null) {
            priceDropThresholdPercent = new BigDecimal("10.00");
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
