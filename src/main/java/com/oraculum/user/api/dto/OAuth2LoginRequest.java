package com.oraculum.user.api.dto;

import java.util.Map;

public record OAuth2LoginRequest(
        String email,
        String firstName,
        String lastName,
        String provider,
        Map<String, Object> attributes
) {
}
