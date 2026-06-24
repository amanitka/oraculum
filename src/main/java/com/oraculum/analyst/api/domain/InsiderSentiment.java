package com.oraculum.analyst.api.domain;

import lombok.Getter;

@Getter
public enum InsiderSentiment {
    HIGHLY_BULLISH("Highly Bullish"),
    MILDLY_BULLISH("Mildly Bullish"),
    NEUTRAL("Neutral"),
    ROUTINE_SELLING("Routine Selling"),
    CONCERNING_SELLING("Concerning Selling");

    private final String displayName;

    InsiderSentiment(String displayName) {
        this.displayName = displayName;
    }
}
