package com.findoraai.giftfinder.organization.controller;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.organization.dto.*;
import com.findoraai.giftfinder.organization.service.OrganizationService;
import com.findoraai.giftfinder.notifications.model.Recipient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orgs")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    public ResponseEntity<OrganizationResponse> createOrganization(
            @Valid @RequestBody OrganizationRequest request,
            @AuthenticationPrincipal User user) {
        OrganizationResponse response = organizationService.createOrganization(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<OrganizationMemberResponse> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal User user) {
        OrganizationMemberResponse response = organizationService.addMember(id, request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/recipients")
    public ResponseEntity<List<Recipient>> getOrganizationRecipients(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        List<Recipient> recipients = organizationService.getOrganizationRecipients(id, user.getId());
        return ResponseEntity.ok(recipients);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getOrganization(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        OrganizationResponse response = organizationService.getOrganization(id, user.getId());
        return ResponseEntity.ok(response);
    }
}
