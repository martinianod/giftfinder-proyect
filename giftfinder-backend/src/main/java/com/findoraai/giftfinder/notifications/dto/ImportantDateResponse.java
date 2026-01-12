package com.findoraai.giftfinder.notifications.dto;

import com.findoraai.giftfinder.notifications.model.ImportantDate;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ImportantDateResponse(
    Long id,
    String name,
    ImportantDate.DateType type,
    LocalDate date,
    Boolean recurring,
    String description,
    Long recipientId,
    String recipientName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
