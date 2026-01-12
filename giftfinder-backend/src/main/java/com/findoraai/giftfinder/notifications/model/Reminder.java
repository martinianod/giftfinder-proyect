package com.findoraai.giftfinder.notifications.model;

import com.findoraai.giftfinder.auth.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reminders", indexes = {
    @Index(name = "idx_reminder_user_scheduled_date", columnList = "user_id,scheduled_date"),
    @Index(name = "idx_reminder_status_scheduled_date", columnList = "status,scheduled_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "important_date_id", nullable = false)
    private ImportantDate importantDate;

    @Column(nullable = false)
    private LocalDate scheduledDate;

    @Column(nullable = false)
    private Integer daysBefore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReminderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    private LocalDateTime sentAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum ReminderStatus {
        PENDING,
        SENT,
        FAILED,
        CANCELLED
    }

    public enum NotificationChannel {
        EMAIL,
        PUSH,
        WHATSAPP
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = ReminderStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
