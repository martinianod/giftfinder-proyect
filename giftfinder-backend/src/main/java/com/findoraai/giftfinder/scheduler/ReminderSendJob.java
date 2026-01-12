package com.findoraai.giftfinder.scheduler;

import com.findoraai.giftfinder.notifications.model.Reminder;
import com.findoraai.giftfinder.notifications.repository.ReminderRepository;
import com.findoraai.giftfinder.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderSendJob {

    private final ReminderRepository reminderRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "${scheduler.reminders.cron:0 0 6 * * *}")
    @Transactional
    public void sendDueReminders() {
        log.info("Starting reminder send job");
        long startTime = System.currentTimeMillis();
        int sentCount = 0;
        int failedCount = 0;

        try {
            LocalDate today = LocalDate.now();
            List<Reminder> dueReminders = reminderRepository.findDueReminders(
                today, Reminder.ReminderStatus.PENDING
            );
            
            log.info("Found {} due reminders to send", dueReminders.size());

            for (Reminder reminder : dueReminders) {
                try {
                    // Check for duplicate notification (within last 24 hours)
                    String referenceId = String.format("reminder-%d-%d", 
                        reminder.getImportantDate().getId(), reminder.getDaysBefore());
                    
                    if (notificationService.wasRecentlySent(
                            reminder.getUser(),
                            com.findoraai.giftfinder.notifications.model.NotificationLog.NotificationType.REMINDER,
                            referenceId,
                            reminder.getChannel())) {
                        log.info("Skipping duplicate reminder {}", reminder.getId());
                        reminder.setStatus(Reminder.ReminderStatus.SENT);
                        reminderRepository.save(reminder);
                        continue;
                    }

                    // Send the notification
                    String eventName = reminder.getImportantDate().getName();
                    String eventDate = reminder.getImportantDate().getDate()
                        .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
                    int daysUntil = (int) ChronoUnit.DAYS.between(today, reminder.getImportantDate().getDate());
                    String recipientName = reminder.getImportantDate().getRecipient() != null ?
                        reminder.getImportantDate().getRecipient().getName() : null;

                    boolean sent = notificationService.sendReminderNotification(
                        reminder.getUser(),
                        reminder,
                        eventName,
                        eventDate,
                        daysUntil,
                        recipientName
                    );

                    if (sent) {
                        reminder.setStatus(Reminder.ReminderStatus.SENT);
                        reminder.setSentAt(java.time.LocalDateTime.now());
                        sentCount++;
                    } else {
                        reminder.setStatus(Reminder.ReminderStatus.FAILED);
                        failedCount++;
                    }

                    reminderRepository.save(reminder);

                } catch (Exception e) {
                    log.error("Error sending reminder {}: {}", reminder.getId(), e.getMessage());
                    reminder.setStatus(Reminder.ReminderStatus.FAILED);
                    reminderRepository.save(reminder);
                    failedCount++;
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Reminder send job completed. Sent: {}, Failed: {}, Duration: {}ms", 
                sentCount, failedCount, duration);

        } catch (Exception e) {
            log.error("Error during reminder send job: {}", e.getMessage(), e);
        }
    }
}
