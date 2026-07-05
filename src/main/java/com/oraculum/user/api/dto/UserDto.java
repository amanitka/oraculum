package com.oraculum.user.api.dto;

import java.time.OffsetDateTime;

public record UserDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        String displayName,
        String provider,
        String role,
        String analysisLimit,
        boolean enabled,
        OffsetDateTime lastLoginAt
) {
}
