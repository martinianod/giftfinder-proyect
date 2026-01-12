package com.findoraai.giftfinder.scheduler;

import com.findoraai.giftfinder.notifications.model.ImportantDate;
import com.findoraai.giftfinder.notifications.model.NotificationPreferences;
import com.findoraai.giftfinder.notifications.model.Reminder;
import com.findoraai.giftfinder.notifications.repository.ImportantDateRepository;
import com.findoraai.giftfinder.notifications.repository.NotificationPreferencesRepository;
import com.findoraai.giftfinder.notifications.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderGenerationJob {

    private final ImportantDateRepository importantDateRepository;
    private final ReminderRepository reminderRepository;
    private final NotificationPreferencesRepository preferencesRepository;

    @Scheduled(cron = "${scheduler.reminders.cron:0 0 6 * * *}")
    @Transactional
    public void generateReminders() {
        log.info("Starting reminder generation job");
        long startTime = System.currentTimeMillis();
        int generatedCount = 0;

        try {
            // Get all upcoming dates for the next 30 days
            LocalDate today = LocalDate.now();
            LocalDate endDate = today.plusDays(30);
            
            List<ImportantDate> upcomingDates = importantDateRepository.findDatesBetween(today, endDate);
            log.info("Found {} upcoming dates to process", upcomingDates.size());

            for (ImportantDate date : upcomingDates) {
                // Get user's notification preferences
                NotificationPreferences prefs = preferencesRepository.findByUser(date.getUser())
                    .orElse(null);
                
                if (prefs == null || !prefs.getRemindersEnabled()) {
                    continue;
                }

                // Generate reminders based on user's preferences
                List<Integer> daysBefore = prefs.getReminderDaysBefore();
                if (daysBefore == null || daysBefore.isEmpty()) {
                    daysBefore = List.of(14, 7, 2); // Default
                }

                for (Integer days : daysBefore) {
                    LocalDate reminderDate = date.getDate().minusDays(days);
                    
                    // Only create reminders for future dates
                    if (reminderDate.isBefore(today)) {
                        continue;
                    }

                    // Check if reminder already exists (idempotency)
                    boolean exists = reminderRepository.existsByUserAndImportantDateAndDaysBeforeAndScheduledDate(
                        date.getUser(), date, days, reminderDate
                    );

                    if (!exists) {
                        Reminder reminder = Reminder.builder()
                            .user(date.getUser())
                            .importantDate(date)
                            .scheduledDate(reminderDate)
                            .daysBefore(days)
                            .status(Reminder.ReminderStatus.PENDING)
                            .channel(prefs.getPreferredChannel())
                            .build();

                        reminderRepository.save(reminder);
                        generatedCount++;
                    }
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Reminder generation job completed successfully. Generated {} new reminders in {}ms", 
                generatedCount, duration);

        } catch (Exception e) {
            log.error("Error during reminder generation job: {}", e.getMessage(), e);
        }
    }
}
