package com.findoraai.giftfinder.organization.service;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.auth.repository.UserRepository;
import com.findoraai.giftfinder.notifications.model.Recipient;
import com.findoraai.giftfinder.notifications.repository.RecipientRepository;
import com.findoraai.giftfinder.organization.dto.*;
import com.findoraai.giftfinder.organization.model.Organization;
import com.findoraai.giftfinder.organization.model.OrganizationMember;
import com.findoraai.giftfinder.organization.model.OrganizationRole;
import com.findoraai.giftfinder.organization.repository.OrganizationMemberRepository;
import com.findoraai.giftfinder.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;
    private final RecipientRepository recipientRepository;

    @Override
    @Transactional
    public OrganizationResponse createOrganization(OrganizationRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Organization organization = Organization.builder()
                .name(request.getName())
                .description(request.getDescription())
                .giftBudget(request.getGiftBudget())
                .build();

        organization = organizationRepository.save(organization);

        // Add creator as OWNER
        OrganizationMember ownerMember = OrganizationMember.builder()
                .organization(organization)
                .user(user)
                .role(OrganizationRole.OWNER)
                .build();

        organizationMemberRepository.save(ownerMember);

        return mapToResponse(organization);
    }

    @Override
    @Transactional
    public OrganizationMemberResponse addMember(Long organizationId, AddMemberRequest request, Long currentUserId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));

        // Check if current user is OWNER or ADMIN
        if (!isOwnerOrAdmin(organizationId, currentUserId)) {
            throw new SecurityException("Only OWNER or ADMIN can add members");
        }

        User newMember = userRepository.findByEmail(request.getUserEmail())
                .orElseThrow(() -> new IllegalArgumentException("User with email " + request.getUserEmail() + " not found"));

        // Check if already a member
        if (organizationMemberRepository.findByOrganizationAndUser(organization, newMember).isPresent()) {
            throw new IllegalArgumentException("User is already a member of this organization");
        }

        OrganizationMember member = OrganizationMember.builder()
                .organization(organization)
                .user(newMember)
                .role(request.getRole())
                .build();

        member = organizationMemberRepository.save(member);

        return mapToMemberResponse(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Recipient> getOrganizationRecipients(Long organizationId, Long currentUserId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        // Check if current user is a member
        if (!isMember(organizationId, currentUserId)) {
            throw new SecurityException("Only organization members can view recipients");
        }

        return recipientRepository.findByOrganization(organization);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationResponse getOrganization(Long organizationId, Long currentUserId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        // Check if current user is a member
        if (!isMember(organizationId, currentUserId)) {
            throw new SecurityException("Only organization members can view organization details");
        }

        return mapToResponse(organization);
    }

    @Override
    public boolean isOwnerOrAdmin(Long organizationId, Long userId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return organizationMemberRepository.findByOrganizationAndUser(organization, user)
                .map(member -> member.getRole() == OrganizationRole.OWNER || 
                              member.getRole() == OrganizationRole.ADMIN)
                .orElse(false);
    }

    @Override
    public boolean isMember(Long organizationId, Long userId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return organizationMemberRepository.findByOrganizationAndUser(organization, user).isPresent();
    }

    private OrganizationResponse mapToResponse(Organization organization) {
        return OrganizationResponse.builder()
                .id(organization.getId())
                .name(organization.getName())
                .description(organization.getDescription())
                .giftBudget(organization.getGiftBudget())
                .createdAt(organization.getCreatedAt())
                .updatedAt(organization.getUpdatedAt())
                .build();
    }

    private OrganizationMemberResponse mapToMemberResponse(OrganizationMember member) {
        return OrganizationMemberResponse.builder()
                .id(member.getId())
                .userId(member.getUser().getId())
                .userEmail(member.getUser().getEmail())
                .userName(member.getUser().getName())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
