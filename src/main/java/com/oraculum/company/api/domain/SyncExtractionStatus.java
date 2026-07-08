package com.oraculum.company.api.domain;

import java.util.Optional;

public enum SyncExtractionStatus {
    FULL,
    PARTIAL,
    EMPTY;

    public static Optional<SyncExtractionStatus> fromString(String value) {
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
