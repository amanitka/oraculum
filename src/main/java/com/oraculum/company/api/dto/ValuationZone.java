package com.oraculum.company.api.dto;

public enum ValuationZone {
    DEEP_DISCOUNT("Deep Discount"),
    FAIR("Fair Value"),
    AGGRESSIVE("Aggressive"),
    SPECULATIVE("Speculative");

    private final String displayName;

    ValuationZone(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
