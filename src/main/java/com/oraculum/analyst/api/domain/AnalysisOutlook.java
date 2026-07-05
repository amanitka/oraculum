package com.oraculum.analyst.api.domain;

import lombok.Getter;

@Getter
public enum AnalysisOutlook {
    BULLISH("Bullish"),
    BEARISH("Bearish"),
    NEUTRAL("Neutral");

    private final String displayName;

    AnalysisOutlook(String displayName) {
        this.displayName = displayName;
    }
}
