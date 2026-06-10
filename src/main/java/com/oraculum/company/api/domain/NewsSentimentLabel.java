package com.oraculum.company.api.domain;

import lombok.Getter;

@Getter
public enum NewsSentimentLabel {
    BEARISH("Bearish"),
    SOMEWHAT_BEARISH("Somewhat-Bearish"),
    NEUTRAL("Neutral"),
    SOMEWHAT_BULLISH("Somewhat-Bullish"),
    BULLISH("Bullish");

    private final String displayName;

    NewsSentimentLabel(String displayName) {
        this.displayName = displayName;
    }

    public static NewsSentimentLabel fromCode(String code) {
        if (code == null) return null;
        try {
            return NewsSentimentLabel.valueOf(code.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
