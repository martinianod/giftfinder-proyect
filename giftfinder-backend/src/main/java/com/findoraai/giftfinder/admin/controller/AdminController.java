package com.findoraai.giftfinder.admin.controller;

import com.findoraai.giftfinder.admin.dto.JobStatusResponse;
import com.findoraai.giftfinder.admin.dto.ReminderQueueResponse;
import com.findoraai.giftfinder.notifications.model.Reminder;
import com.findoraai.giftfinder.notifications.repository.ReminderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ReminderRepository reminderRepository;

    @GetMapping("/job-status")
    public ResponseEntity<Map<String, String>> getJobStatus() {
        // Simple status endpoint - in production, track actual job executions
        return ResponseEntity.ok(Map.of(
            "reminderGenerationJob", "Active",
            "reminderSendJob", "Active",
            "priceCheckJob", "Active",
            "schedulerStatus", "Running"
        ));
    }

    @GetMapping("/reminders/queue")
    public ResponseEntity<List<ReminderQueueResponse>> getReminderQueue(
            @RequestParam(required = false, defaultValue = "PENDING") String status) {
        
        Reminder.ReminderStatus reminderStatus;
        try {
            reminderStatus = Reminder.ReminderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            reminderStatus = Reminder.ReminderStatus.PENDING;
        }

        List<ReminderQueueResponse> reminders = reminderRepository.findByStatusAndScheduledDate(
            reminderStatus, LocalDate.now()
        ).stream()
            .map(r -> new ReminderQueueResponse(
                r.getId(),
                r.getUser().getId(),
                r.getUser().getEmail(),
                r.getImportantDate().getName(),
                r.getImportantDate().getDate(),
                r.getScheduledDate(),
                r.getDaysBefore(),
                r.getStatus(),
                r.getChannel()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(reminders);
    }

    @GetMapping("/reminders/upcoming")
    public ResponseEntity<List<ReminderQueueResponse>> getUpcomingReminders(
            @RequestParam(required = false, defaultValue = "7") int days) {
        
        LocalDate endDate = LocalDate.now().plusDays(days);
        List<ReminderQueueResponse> reminders = reminderRepository.findDueReminders(
            endDate, Reminder.ReminderStatus.PENDING
        ).stream()
            .map(r -> new ReminderQueueResponse(
                r.getId(),
                r.getUser().getId(),
                r.getUser().getEmail(),
                r.getImportantDate().getName(),
                r.getImportantDate().getDate(),
                r.getScheduledDate(),
                r.getDaysBefore(),
                r.getStatus(),
                r.getChannel()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(reminders);
    }

    @GetMapping("/reminders/stats")
    public ResponseEntity<Map<String, Long>> getReminderStats() {
        List<Reminder> allReminders = reminderRepository.findAll();
        
        Map<String, Long> stats = Map.of(
            "total", (long) allReminders.size(),
            "pending", allReminders.stream().filter(r -> r.getStatus() == Reminder.ReminderStatus.PENDING).count(),
            "sent", allReminders.stream().filter(r -> r.getStatus() == Reminder.ReminderStatus.SENT).count(),
            "failed", allReminders.stream().filter(r -> r.getStatus() == Reminder.ReminderStatus.FAILED).count(),
            "cancelled", allReminders.stream().filter(r -> r.getStatus() == Reminder.ReminderStatus.CANCELLED).count()
        );

        return ResponseEntity.ok(stats);
    }
}
