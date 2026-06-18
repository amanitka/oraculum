package com.oraculum.harvester.provider.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AlphaVantageEarningsEstimatesResponse(
        String symbol,
        List<EarningsEstimate> estimates
) {
    public record EarningsEstimate(
            LocalDate date,
            String horizon,
            @JsonProperty("eps_estimate_average") BigDecimal epsEstimateAverage,
            @JsonProperty("eps_estimate_high") BigDecimal epsEstimateHigh,
            @JsonProperty("eps_estimate_low") BigDecimal epsEstimateLow,
            @JsonProperty("eps_estimate_analyst_count") BigDecimal epsEstimateAnalystCount,
            @JsonProperty("eps_estimate_average_7_days_ago") BigDecimal epsEstimateAverage7DaysAgo,
            @JsonProperty("eps_estimate_average_30_days_ago") BigDecimal epsEstimateAverage30DaysAgo,
            @JsonProperty("eps_estimate_average_60_days_ago") BigDecimal epsEstimateAverage60DaysAgo,
            @JsonProperty("eps_estimate_average_90_days_ago") BigDecimal epsEstimateAverage90DaysAgo,
            @JsonProperty("eps_estimate_revision_up_trailing_7_days") BigDecimal epsEstimateRevisionUpTrailing7Days,
            @JsonProperty("eps_estimate_revision_down_trailing_7_days") BigDecimal epsEstimateRevisionDownTrailing7Days,
            @JsonProperty("eps_estimate_revision_up_trailing_30_days") BigDecimal epsEstimateRevisionUpTrailing30Days,
            @JsonProperty("eps_estimate_revision_down_trailing_30_days") BigDecimal epsEstimateRevisionDownTrailing30Days,
            @JsonProperty("revenue_estimate_average") BigDecimal revenueEstimateAverage,
            @JsonProperty("revenue_estimate_high") BigDecimal revenueEstimateHigh,
            @JsonProperty("revenue_estimate_low") BigDecimal revenueEstimateLow,
            @JsonProperty("revenue_estimate_analyst_count") BigDecimal revenueEstimateAnalystCount
    ) {}
}
