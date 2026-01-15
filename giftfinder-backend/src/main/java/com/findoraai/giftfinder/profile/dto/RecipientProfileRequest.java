package com.findoraai.giftfinder.profile.dto;

import com.findoraai.giftfinder.profile.model.RecipientProfile;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipientProfileRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 100, message = "Relationship must not exceed 100 characters")
    private String relationship;

    @Size(max = 2000, message = "Interests must not exceed 2000 characters")
    private String interests;

    @Size(max = 1000, message = "Restrictions must not exceed 1000 characters")
    private String restrictions;

    @Size(max = 500, message = "Sizes must not exceed 500 characters")
    private String sizes;

    @Size(max = 500, message = "Preferred stores must not exceed 500 characters")
    private String preferredStores;

    @DecimalMin(value = "0.0", inclusive = false, message = "Budget min must be positive")
    private BigDecimal budgetMin;

    @DecimalMin(value = "0.0", inclusive = false, message = "Budget max must be positive")
    private BigDecimal budgetMax;

    @NotNull(message = "Visibility is required")
    private RecipientProfile.ProfileVisibility visibility;

    @Email(message = "Invalid email format")
    private String claimEmail;
}
