package com.oraculum.company.api.domain;

import lombok.Getter;

@Getter
public enum ScreenerSignal {
    HIGH_CONVICTION("High Conviction"),
    FAVORABLE("Favorable"),
    NEUTRAL("Neutral"),
    CAUTIOUS("Cautious");

    private final String displayName;

    ScreenerSignal(String displayName) {
        this.displayName = displayName;
    }

    public static ScreenerSignal fromCode(String code) {
        if (code == null) return null;
        try {
            return ScreenerSignal.valueOf(code.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
