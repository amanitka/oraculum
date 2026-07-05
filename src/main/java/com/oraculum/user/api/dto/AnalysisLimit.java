package com.oraculum.user.api.dto;

import org.jspecify.annotations.NonNull;

public record AnalysisLimit(int count, Period period) {

    public static AnalysisLimit parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        value = value.trim().toUpperCase();
        int count = Integer.parseInt(value.substring(0, value.length() - 1));
        Period period = Period.valueOf(String.valueOf(value.charAt(value.length() - 1)));
        return new AnalysisLimit(count, period);
    }

    @Override
    public @NonNull String toString() {
        return count + period.name();
    }

    public enum Period {
        D, // Daily
        W, // Weekly
        M  // Monthly
    }
}
