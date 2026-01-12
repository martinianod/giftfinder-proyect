package com.findoraai.giftfinder.notifications.model;

import com.findoraai.giftfinder.auth.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "important_dates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportantDate {

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
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DateType type;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Boolean recurring;

    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum DateType {
        BIRTHDAY,
        HOLIDAY,
        ANNIVERSARY,
        CUSTOM
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (recurring == null) {
            recurring = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
