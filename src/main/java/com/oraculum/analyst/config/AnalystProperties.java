package com.oraculum.analyst.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;

@ConfigurationProperties(prefix = "oraculum.analyst")
public record AnalystProperties(Refresh refresh,
                                Cleanup cleanup,
                                FactSheet factSheet,
                                SharePrice sharePrice,
                                News news,
                                int tokenBudget) {
    public record Refresh(String priceCron,
                          String fundamentalsCron,
                          String tickerCron) {
    }

    public record Cleanup(String dataCleanupCron,
                          int dataRetentionDays) {
    }

    public record FactSheet(int historyLimit) {
        public LocalDate getFactSheetHistoryDate() {
            return LocalDate.now().minusDays(historyLimit);
        }
    }

    public record SharePrice(int dailyHistoryLimit,
                             int monthlyHistoryLimit) {
        public LocalDate getSharePriceHistoryDate() {
            return LocalDate.now().minusDays(dailyHistoryLimit);
        }

        public LocalDate getMonthlySharePriceHistoryDate() {
            return LocalDate.now().minusDays(monthlyHistoryLimit);
        }
    }

    public record News(int historyLimit) {
        public LocalDate getNewsHistoryDate() {
            return LocalDate.now().minusDays(historyLimit);
        }
    }
}