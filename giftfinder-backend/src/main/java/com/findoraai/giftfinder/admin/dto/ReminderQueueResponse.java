package com.findoraai.giftfinder.admin.dto;

import com.findoraai.giftfinder.notifications.model.Reminder;

import java.time.LocalDate;

public record ReminderQueueResponse(
    Long reminderId,
    Long userId,
    String userEmail,
    String eventName,
    LocalDate eventDate,
    LocalDate scheduledDate,
    Integer daysBefore,
    Reminder.ReminderStatus status,
    Reminder.NotificationChannel channel
) {}
