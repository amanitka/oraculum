package com.oraculum.economy.api.domain;

import lombok.Getter;

@Getter
public enum MacroIndicator {
    FEDFUNDS("Fed Funds Effective Rate"),
    T10Y2Y("10-Year Minus 2-Year Spread"),
    CPIAUCSL("Consumer Price Index (All Urban)"),
    UNRATE("Civilian Unemployment Rate"),
    RSAFS("Retail Sales: Total"),
    DGORDER("New Orders: Durable Goods"),
    GS10("10-Year Treasury Constant Maturity"),
    GDPC1("Real Gross Domestic Product"),
    M2SL("M2 Money Supply"),
    PPIACO("Producer Price Index for All Commodities"),
    INDPRO("Industrial Production: Total Index"),
    HOUST("Housing Starts: Total"),
    UMCSENT("University of Michigan: Consumer Sentiment"),
    DCOILWTICO("Crude Oil Prices: WTI"),
    MORTGAGE30US("30-Year Fixed Rate Mortgage Average"),
    TCU("Capacity Utilization: Total Industry");

    private final String title;

    MacroIndicator(String title) {
        this.title = title;
    }
}
