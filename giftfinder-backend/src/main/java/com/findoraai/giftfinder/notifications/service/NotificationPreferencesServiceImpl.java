package com.findoraai.giftfinder.notifications.service;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.notifications.dto.NotificationPreferencesRequest;
import com.findoraai.giftfinder.notifications.dto.NotificationPreferencesResponse;
import com.findoraai.giftfinder.notifications.model.NotificationPreferences;
import com.findoraai.giftfinder.notifications.model.Reminder;
import com.findoraai.giftfinder.notifications.repository.NotificationPreferencesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationPreferencesServiceImpl implements NotificationPreferencesService {

    private final NotificationPreferencesRepository preferencesRepository;

    @Override
    @Transactional(readOnly = true)
    public NotificationPreferencesResponse getUserPreferences(User user) {
        NotificationPreferences prefs = preferencesRepository.findByUser(user)
            .orElseGet(() -> createDefaultPreferences(user));
        
        return toResponse(prefs);
    }

    @Override
    @Transactional
    public NotificationPreferencesResponse updatePreferences(NotificationPreferencesRequest request, User user) {
        NotificationPreferences prefs = preferencesRepository.findByUser(user)
            .orElseGet(() -> createDefaultPreferences(user));
        
        prefs.setRemindersEnabled(request.remindersEnabled());
        prefs.setPriceDropAlertsEnabled(request.priceDropAlertsEnabled());
        prefs.setReminderDaysBefore(request.reminderDaysBefore());
        prefs.setPreferredChannel(request.preferredChannel());
        
        prefs = preferencesRepository.save(prefs);
        return toResponse(prefs);
    }

    private NotificationPreferences createDefaultPreferences(User user) {
        NotificationPreferences prefs = NotificationPreferences.builder()
            .user(user)
            .remindersEnabled(true)
            .priceDropAlertsEnabled(true)
            .reminderDaysBefore(List.of(14, 7, 2))
            .preferredChannel(Reminder.NotificationChannel.EMAIL)
            .build();
        
        return preferencesRepository.save(prefs);
    }

    private NotificationPreferencesResponse toResponse(NotificationPreferences prefs) {
        return new NotificationPreferencesResponse(
            prefs.getId(),
            prefs.getRemindersEnabled(),
            prefs.getPriceDropAlertsEnabled(),
            prefs.getReminderDaysBefore(),
            prefs.getPreferredChannel(),
            prefs.getCreatedAt(),
            prefs.getUpdatedAt()
        );
    }
}
