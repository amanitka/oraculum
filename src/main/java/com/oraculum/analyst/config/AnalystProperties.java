package com.oraculum.analyst.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;

@ConfigurationProperties(prefix = "oraculum.analyst")
public record AnalystProperties(Refresh refresh,
                                Cleanup cleanup,
                                FactSheet factSheet,
                                SharePrice sharePrice,
                                News news,
                                Critic critic,
                                int tokenBudget) {
    public record Refresh(String priceCron,
                          String fundamentalsCron,
                          String tickerCron) {
    }

    public record Cleanup(String dataCleanupCron,
                          int dataRetentionDays) {
    }

    public record FactSheet(int annualHistoryLimit, int quarterlyHistoryLimit) {
        public LocalDate getAnnualFactSheetHistoryDate() {
            return LocalDate.now().minusDays(annualHistoryLimit);
        }
        public LocalDate getQuarterlyFactSheetHistoryDate() {
            return LocalDate.now().minusDays(quarterlyHistoryLimit);
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

    public record Critic(int maxReruns, int maxSpecialistsPerRerun) {
    }
}