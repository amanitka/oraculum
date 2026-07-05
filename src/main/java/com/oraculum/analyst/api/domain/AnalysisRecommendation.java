package com.oraculum.analyst.api.domain;

import lombok.Getter;

@Getter
public enum AnalysisRecommendation {
    BUY("Buy"),
    SELL("Sell"),
    HOLD("Hold"),
    NEUTRAL("Neutral");

    private final String displayName;

    AnalysisRecommendation(String displayName) {
        this.displayName = displayName;
    }
}
