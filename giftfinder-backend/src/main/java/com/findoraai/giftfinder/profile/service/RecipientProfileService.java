package com.findoraai.giftfinder.profile.service;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.profile.dto.RecipientProfileRequest;
import com.findoraai.giftfinder.profile.dto.RecipientProfileResponse;

import java.util.List;

public interface RecipientProfileService {
    List<RecipientProfileResponse> getUserProfiles(User user);
    RecipientProfileResponse getProfile(Long id, User user);
    RecipientProfileResponse createProfile(RecipientProfileRequest request, User user);
    RecipientProfileResponse updateProfile(Long id, RecipientProfileRequest request, User user);
    void deleteProfile(Long id, User user);
    String generateShareLink(Long id, User user);
}
