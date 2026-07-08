package com.oraculum.company.api.domain;

import java.util.Optional;

public enum SyncStatus {
    PENDING,
    COMPLETED,
    FAILED;

    public static Optional<SyncStatus> fromString(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(value.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
