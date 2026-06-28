package com.oraculum.economy.api.domain;

import lombok.Getter;

@Getter
public enum MacroIndicator {
    FEDFUNDS("Fed Funds Effective Rate", "%"),
    T10Y2Y("10-Year Minus 2-Year Spread", "%"),
    CPIAUCSL("Consumer Price Index (All Urban)", "Index 1982-1984=100"),
    UNRATE("Civilian Unemployment Rate", "%"),
    RSAFS("Retail Sales: Total", "Millions of Dollars"),
    DGORDER("New Orders: Durable Goods", "Millions of Dollars"),
    GS10("10-Year Treasury Constant Maturity", "%"),
    GDPC1("Real Gross Domestic Product", "Billions of Chained Dollars"),
    M2SL("M2 Money Supply", "Billions of Dollars"),
    PPIACO("Producer Price Index for All Commodities", "Index 1982=100"),
    INDPRO("Industrial Production: Total Index", "Index 2017=100"),
    HOUST("Housing Starts: Total", "Thousands of Units"),
    UMCSENT("University of Michigan: Consumer Sentiment", "Index 1966:Q1=100"),
    DCOILWTICO("Crude Oil Prices: WTI", "Dollars per Barrel"),
    MORTGAGE30US("30-Year Fixed Rate Mortgage Average", "%"),
    TCU("Capacity Utilization: Total Industry", "Percent of Capacity");

    private final String title;
    private final String unit;

    MacroIndicator(String title, String unit) {
        this.title = title;
        this.unit = unit;
    }
}
