package com.oraculum.economy.api.domain;

import lombok.Getter;

@Getter
public enum MacroIndicator {
    FEDFUNDS("Fed Funds Effective Rate", "%", SentimentDirection.NEUTRAL),
    T10Y2Y("10-Year Minus 2-Year Spread", "%", SentimentDirection.POSITIVE_IS_GOOD),
    CPIAUCSL("Consumer Price Index (All Urban)", "Index 1982-1984=100", SentimentDirection.NEGATIVE_IS_GOOD),
    UNRATE("Civilian Unemployment Rate", "%", SentimentDirection.NEGATIVE_IS_GOOD),
    RSAFS("Retail Sales: Total", "Millions of Dollars", SentimentDirection.POSITIVE_IS_GOOD),
    DGORDER("New Orders: Durable Goods", "Millions of Dollars", SentimentDirection.POSITIVE_IS_GOOD),
    GS10("10-Year Treasury Constant Maturity", "%", SentimentDirection.NEUTRAL),
    GDPC1("Real Gross Domestic Product", "Billions of Chained Dollars", SentimentDirection.POSITIVE_IS_GOOD),
    M2SL("M2 Money Supply", "Billions of Dollars", SentimentDirection.POSITIVE_IS_GOOD),
    PPIACO("Producer Price Index for All Commodities", "Index 1982=100", SentimentDirection.NEGATIVE_IS_GOOD),
    INDPRO("Industrial Production: Total Index", "Index 2017=100", SentimentDirection.POSITIVE_IS_GOOD),
    HOUST("Housing Starts: Total", "Thousands of Units", SentimentDirection.POSITIVE_IS_GOOD),
    UMCSENT("University of Michigan: Consumer Sentiment", "Index 1966:Q1=100", SentimentDirection.POSITIVE_IS_GOOD),
    DCOILWTICO("Crude Oil Prices: WTI", "Dollars per Barrel", SentimentDirection.NEGATIVE_IS_GOOD),
    MORTGAGE30US("30-Year Fixed Rate Mortgage Average", "%", SentimentDirection.NEGATIVE_IS_GOOD),
    TCU("Capacity Utilization: Total Industry", "Percent of Capacity", SentimentDirection.POSITIVE_IS_GOOD);

    private final String title;
    private final String unit;
    private final SentimentDirection sentimentDirection;

    MacroIndicator(String title, String unit, SentimentDirection sentimentDirection) {
        this.title = title;
        this.unit = unit;
        this.sentimentDirection = sentimentDirection;
    }

    public enum SentimentDirection {
        POSITIVE_IS_GOOD,
        NEGATIVE_IS_GOOD,
        NEUTRAL
    }
}
