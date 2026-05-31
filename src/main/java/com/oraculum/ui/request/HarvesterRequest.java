package com.oraculum.ui.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * Base class and nested subtypes for harvester refresh requests published to Kafka.
 * JSON fields use snake_case to match the Python Pydantic models on the harvester side.
 */
@Getter
public abstract class HarvesterRequest {

    @JsonProperty("correlation_id")
    private final UUID correlationId = UUID.randomUUID();

    @JsonProperty("issued_at")
    private final String issuedAt = java.time.Instant.now().toString();

    @JsonProperty("request_type")
    public abstract String getRequestType();

    // -------------------------------------------------------------------------
    // Concrete request types
    // -------------------------------------------------------------------------

    @Getter
    public static class FetchCompany extends HarvesterRequest {
        private final String market;

        public FetchCompany(String market) {
            this.market = market;
        }

        @Override
        public String getRequestType() {
            return "fetch_company";
        }
    }

    @Getter
    public static class FetchMarket extends HarvesterRequest {
        @Override
        public String getRequestType() {
            return "fetch_market";
        }
    }

    @Getter
    public static class FetchIndustry extends HarvesterRequest {
        @Override
        public String getRequestType() {
            return "fetch_industry";
        }
    }

    @Getter
    public static class FetchSharePrice extends HarvesterRequest {
        private final String market;
        private final String variant;

        @JsonProperty("from_date")
        private final String fromDate;

        @JsonProperty("safety_window_days")
        private final int safetyWindowDays;

        public FetchSharePrice(String market, String variant, String fromDate, int safetyWindowDays) {
            this.market = market;
            this.variant = variant;
            this.fromDate = fromDate;
            this.safetyWindowDays = safetyWindowDays;
        }

        @Override
        public String getRequestType() {
            return "fetch_share_price";
        }
    }

    @Getter
    public static class FetchNews extends HarvesterRequest {
        @JsonProperty("time_from")
        private final String timeFrom;

        public FetchNews(String timeFrom) {
            this.timeFrom = timeFrom;
        }

        @Override
        public String getRequestType() {
            return "fetch_news";
        }
    }

    @Getter
    public static class FetchIncomeStatement extends HarvesterRequest {
        private final String market;
        private final String variant;
        private final List<String> templates;

        public FetchIncomeStatement(String market, String variant, List<String> templates) {
            this.market = market;
            this.variant = variant;
            this.templates = templates;
        }

        @Override
        public String getRequestType() {
            return "fetch_income_statement";
        }
    }

    @Getter
    public static class FetchBalanceSheet extends HarvesterRequest {
        private final String market;
        private final String variant;
        private final List<String> templates;

        public FetchBalanceSheet(String market, String variant, List<String> templates) {
            this.market = market;
            this.variant = variant;
            this.templates = templates;
        }

        @Override
        public String getRequestType() {
            return "fetch_balance_sheet";
        }
    }

    @Getter
    public static class FetchCashFlowStatement extends HarvesterRequest {
        private final String market;
        private final String variant;
        private final List<String> templates;

        public FetchCashFlowStatement(String market, String variant, List<String> templates) {
            this.market = market;
            this.variant = variant;
            this.templates = templates;
        }

        @Override
        public String getRequestType() {
            return "fetch_cash_flow_statement";
        }
    }
}