package com.findoraai.giftfinder.notifications.controller;

import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.notifications.dto.ImportantDateRequest;
import com.findoraai.giftfinder.notifications.dto.ImportantDateResponse;
import com.findoraai.giftfinder.notifications.service.ImportantDateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/important-dates")
@RequiredArgsConstructor
public class ImportantDateController {

    private final ImportantDateService importantDateService;

    @GetMapping
    public ResponseEntity<List<ImportantDateResponse>> getUserDates(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        List<ImportantDateResponse> dates = importantDateService.getUserDates(user);
        return ResponseEntity.ok(dates);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImportantDateResponse> getDate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        ImportantDateResponse date = importantDateService.getDate(id, user);
        return ResponseEntity.ok(date);
    }

    @PostMapping
    public ResponseEntity<ImportantDateResponse> createDate(
            @Valid @RequestBody ImportantDateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        ImportantDateResponse date = importantDateService.createDate(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(date);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ImportantDateResponse> updateDate(
            @PathVariable Long id,
            @Valid @RequestBody ImportantDateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        ImportantDateResponse date = importantDateService.updateDate(id, request, user);
        return ResponseEntity.ok(date);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        importantDateService.deleteDate(id, user);
        return ResponseEntity.noContent().build();
    }
}
