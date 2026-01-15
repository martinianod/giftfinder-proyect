package com.findoraai.giftfinder.organization.service;

import com.findoraai.giftfinder.organization.dto.*;
import com.findoraai.giftfinder.notifications.model.Recipient;

import java.util.List;

public interface OrganizationService {
    OrganizationResponse createOrganization(OrganizationRequest request, Long userId);
    OrganizationMemberResponse addMember(Long organizationId, AddMemberRequest request, Long currentUserId);
    List<Recipient> getOrganizationRecipients(Long organizationId, Long currentUserId);
    OrganizationResponse getOrganization(Long organizationId, Long currentUserId);
    boolean isOwnerOrAdmin(Long organizationId, Long userId);
    boolean isMember(Long organizationId, Long userId);
}
