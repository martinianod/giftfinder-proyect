package com.findoraai.giftfinder.notifications.repository;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.notifications.model.NotificationLog;
import com.findoraai.giftfinder.notifications.model.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    
    Optional<NotificationLog> findByUserAndNotificationTypeAndReferenceIdAndChannelAndSentAtAfter(
        User user, 
        NotificationLog.NotificationType notificationType,
        String referenceId,
        Reminder.NotificationChannel channel,
        LocalDateTime sentAt
    );
    
    boolean existsByUserAndNotificationTypeAndReferenceIdAndChannelAndSentAtAfter(
        User user, 
        NotificationLog.NotificationType notificationType,
        String referenceId,
        Reminder.NotificationChannel channel,
        LocalDateTime sentAt
    );
}
