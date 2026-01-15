package com.findoraai.giftfinder.tracking.model;

import com.findoraai.giftfinder.auth.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbound_clicks", indexes = {
    @Index(name = "idx_outbound_click_user", columnList = "user_id"),
    @Index(name = "idx_outbound_click_anon", columnList = "anonymous_id"),
    @Index(name = "idx_outbound_click_product", columnList = "product_id"),
    @Index(name = "idx_outbound_click_provider", columnList = "provider"),
    @Index(name = "idx_outbound_click_timestamp", columnList = "clicked_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboundClick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String clickId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "anonymous_id", length = 100)
    private String anonymousId;

    @Column(name = "product_id", nullable = false, length = 200)
    private String productId;

    @Column(nullable = false, length = 100)
    private String provider;

    @Column(nullable = false, length = 2000)
    private String targetUrl;

    @Column(length = 100)
    private String campaignId;

    @Column(length = 500)
    private String trackingTags;

    @Column(length = 100)
    private String utmSource;

    @Column(length = 100)
    private String utmMedium;

    @Column(length = 100)
    private String utmCampaign;

    @Column(length = 500)
    private String utmContent;

    @Column(length = 100)
    private String utmTerm;

    @Column(nullable = false)
    private LocalDateTime clickedAt;

    @PrePersist
    protected void onCreate() {
        clickedAt = LocalDateTime.now();
    }
}
