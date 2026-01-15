package com.findoraai.giftfinder.organization.dto;

import com.findoraai.giftfinder.organization.model.OrganizationRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMemberResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private String userName;
    private OrganizationRole role;
    private LocalDateTime joinedAt;
}
