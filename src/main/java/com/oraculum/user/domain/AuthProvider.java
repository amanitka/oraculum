package com.oraculum.user.domain;

public enum AuthProvider {
    GOOGLE,
    GITHUB,
    KEYCLOAK,
    PENDING;

    public static AuthProvider fromString(String provider) {
        if (provider == null || provider.isBlank()) {
            return PENDING;
        }
        try {
            return AuthProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown auth provider: " + provider);
        }
    }
}
