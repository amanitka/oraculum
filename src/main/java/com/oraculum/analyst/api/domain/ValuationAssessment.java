package com.oraculum.analyst.api.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Describes the relationship between current market price and estimated
 * intrinsic value based on fundamental analysis.
 * <p>
 * This is a <strong>valuation assessment</strong>, not a personalised
 * investment recommendation. It does not constitute financial advice
 * under MiFID II or any other regulatory framework.
 */
@Getter
@RequiredArgsConstructor
public enum ValuationAssessment {

    UNDERVALUED("Undervalued",
            "Current price appears to trade below estimated intrinsic value"),
    FAIRLY_VALUED("Fairly Valued",
            "Current price appears to reflect estimated intrinsic value"),
    OVERVALUED("Overvalued",
            "Current price appears to trade above estimated intrinsic value"),
    UNCERTAIN("Uncertain",
            "Insufficient signal to form a clear valuation view");

    private final String displayLabel;
    private final String description;
}
