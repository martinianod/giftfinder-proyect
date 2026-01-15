package com.findoraai.giftfinder.organization.controller;

import com.findoraai.giftfinder.organization.dto.*;
import com.findoraai.giftfinder.organization.service.OrganizationService;
import com.findoraai.giftfinder.notifications.model.Recipient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orgs")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    public ResponseEntity<OrganizationResponse> createOrganization(
            @Valid @RequestBody OrganizationRequest request) {
        Long userId = getCurrentUserId();
        OrganizationResponse response = organizationService.createOrganization(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<OrganizationMemberResponse> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddMemberRequest request) {
        Long userId = getCurrentUserId();
        OrganizationMemberResponse response = organizationService.addMember(id, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/recipients")
    public ResponseEntity<List<Recipient>> getOrganizationRecipients(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        List<Recipient> recipients = organizationService.getOrganizationRecipients(id, userId);
        return ResponseEntity.ok(recipients);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getOrganization(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        OrganizationResponse response = organizationService.getOrganization(id, userId);
        return ResponseEntity.ok(response);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            throw new SecurityException("User not authenticated");
        }
        // Assuming the principal contains user ID or email
        // This might need to be adjusted based on your JWT implementation
        return 1L; // Placeholder - should be extracted from JWT claims
    }
}
