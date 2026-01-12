package com.findoraai.giftfinder.notifications.service;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.notifications.dto.RecipientRequest;
import com.findoraai.giftfinder.notifications.dto.RecipientResponse;

import java.util.List;

public interface RecipientService {
    List<RecipientResponse> getUserRecipients(User user);
    RecipientResponse getRecipient(Long id, User user);
    RecipientResponse createRecipient(RecipientRequest request, User user);
    RecipientResponse updateRecipient(Long id, RecipientRequest request, User user);
    void deleteRecipient(Long id, User user);
}
