package com.findoraai.giftfinder.notifications.controller;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.notifications.dto.RecipientRequest;
import com.findoraai.giftfinder.notifications.dto.RecipientResponse;
import com.findoraai.giftfinder.notifications.service.RecipientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recipients")
@RequiredArgsConstructor
public class RecipientController {

    private final RecipientService recipientService;

    @GetMapping
    public ResponseEntity<List<RecipientResponse>> getUserRecipients(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        List<RecipientResponse> recipients = recipientService.getUserRecipients(user);
        return ResponseEntity.ok(recipients);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipientResponse> getRecipient(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        RecipientResponse recipient = recipientService.getRecipient(id, user);
        return ResponseEntity.ok(recipient);
    }

    @PostMapping
    public ResponseEntity<RecipientResponse> createRecipient(
            @Valid @RequestBody RecipientRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        RecipientResponse recipient = recipientService.createRecipient(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(recipient);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecipientResponse> updateRecipient(
            @PathVariable Long id,
            @Valid @RequestBody RecipientRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        RecipientResponse recipient = recipientService.updateRecipient(id, request, user);
        return ResponseEntity.ok(recipient);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipient(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        recipientService.deleteRecipient(id, user);
        return ResponseEntity.noContent().build();
    }
}
