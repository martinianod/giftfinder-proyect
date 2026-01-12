package com.findoraai.giftfinder.notifications.service;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.notifications.dto.ImportantDateRequest;
import com.findoraai.giftfinder.notifications.dto.ImportantDateResponse;

import java.util.List;

public interface ImportantDateService {
    List<ImportantDateResponse> getUserDates(User user);
    ImportantDateResponse getDate(Long id, User user);
    ImportantDateResponse createDate(ImportantDateRequest request, User user);
    ImportantDateResponse updateDate(Long id, ImportantDateRequest request, User user);
    void deleteDate(Long id, User user);
}
