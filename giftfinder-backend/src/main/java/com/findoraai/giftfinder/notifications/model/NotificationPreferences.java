package com.findoraai.giftfinder.notifications.model;

import com.findoraai.giftfinder.auth.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Boolean remindersEnabled;

    @Column(nullable = false)
    private Boolean priceDropAlertsEnabled;

    @ElementCollection
    @CollectionTable(name = "reminder_days_before", joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "days_before")
    @Builder.Default
    private List<Integer> reminderDaysBefore = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Reminder.NotificationChannel preferredChannel;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (remindersEnabled == null) {
            remindersEnabled = true;
        }
        if (priceDropAlertsEnabled == null) {
            priceDropAlertsEnabled = true;
        }
        if (preferredChannel == null) {
            preferredChannel = Reminder.NotificationChannel.EMAIL;
        }
        if (reminderDaysBefore == null || reminderDaysBefore.isEmpty()) {
            reminderDaysBefore = List.of(14, 7, 2);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
