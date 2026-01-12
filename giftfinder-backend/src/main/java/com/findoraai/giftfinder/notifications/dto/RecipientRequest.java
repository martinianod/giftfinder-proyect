package com.findoraai.giftfinder.notifications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RecipientRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be less than 255 characters")
    String name,
    
    @Size(max = 1000, message = "Description must be less than 1000 characters")
    String description,
    
    LocalDate birthday
) {}
