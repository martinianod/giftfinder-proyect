package com.findoraai.giftfinder.notifications.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RecipientResponse(
    Long id,
    String name,
    String description,
    LocalDate birthday,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
