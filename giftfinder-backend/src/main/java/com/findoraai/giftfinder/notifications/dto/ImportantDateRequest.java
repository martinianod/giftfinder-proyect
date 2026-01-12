package com.findoraai.giftfinder.notifications.dto;

import com.findoraai.giftfinder.notifications.model.ImportantDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ImportantDateRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be less than 255 characters")
    String name,
    
    @NotNull(message = "Type is required")
    ImportantDate.DateType type,
    
    @NotNull(message = "Date is required")
    LocalDate date,
    
    Boolean recurring,
    
    @Size(max = 1000, message = "Description must be less than 1000 characters")
    String description,
    
    Long recipientId
) {}
