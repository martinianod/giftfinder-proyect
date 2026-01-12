package com.findoraai.giftfinder.notifications.dto;

import com.findoraai.giftfinder.notifications.model.Reminder;

import java.time.LocalDateTime;
import java.util.List;

public record NotificationPreferencesResponse(
    Long id,
    Boolean remindersEnabled,
    Boolean priceDropAlertsEnabled,
    List<Integer> reminderDaysBefore,
    Reminder.NotificationChannel preferredChannel,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
