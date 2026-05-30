package com.oraculum.analyst.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oraculum.analyst")
public record AnalystProperties(
    Refresh refresh,
    Cleanup cleanup,
    FactSheet factSheet
) {
    public record Refresh(
        String priceCron,
        String fundamentalsCron,
        String tickerCron
    ) {}

    public record Cleanup(
        String dataCleanupCron,
        int dataRetentionDays
    ) {}

    public record FactSheet(
        int historyLimit
    ) {}
}