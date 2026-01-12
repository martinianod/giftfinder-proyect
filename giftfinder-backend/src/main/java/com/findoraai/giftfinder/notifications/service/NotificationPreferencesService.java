package com.findoraai.giftfinder.notifications.service;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.notifications.dto.NotificationPreferencesRequest;
import com.findoraai.giftfinder.notifications.dto.NotificationPreferencesResponse;

public interface NotificationPreferencesService {
    NotificationPreferencesResponse getUserPreferences(User user);
    NotificationPreferencesResponse updatePreferences(NotificationPreferencesRequest request, User user);
}
