package com.findoraai.giftfinder.admin.dto;

import java.time.LocalDateTime;

public record JobStatusResponse(
    String jobName,
    String status,
    Long totalProcessed,
    Long successCount,
    Long failureCount,
    LocalDateTime lastRun,
    Long durationMs
) {}
