package com.findoraai.giftfinder.notifications.dto;

import com.findoraai.giftfinder.notifications.model.Reminder;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record NotificationPreferencesRequest(
    @NotNull(message = "Reminders enabled flag is required")
    Boolean remindersEnabled,
    
    @NotNull(message = "Price drop alerts enabled flag is required")
    Boolean priceDropAlertsEnabled,
    
    @NotEmpty(message = "At least one reminder day must be specified")
    List<Integer> reminderDaysBefore,
    
    @NotNull(message = "Preferred channel is required")
    Reminder.NotificationChannel preferredChannel
) {}
