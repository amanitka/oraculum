package com.oraculum.company.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReverseDcfDto(
        @JsonProperty("current_market_cap") Double currentMarketCap,
        @JsonProperty("current_fcf") Double currentFcf,
        @JsonProperty("fcf_yield_pct") Double fcfYieldPct,
        @JsonProperty("discount_rate_pct") Double discountRatePct,
        @JsonProperty("projection_years") int projectionYears,
        @JsonProperty("terminal_growth_rate_pct") Double terminalGrowthRatePct,
        @JsonProperty("implied_fcf_growth_rate_pct") Double impliedFcfGrowthRatePct,
        @JsonProperty("historical_fcf_cagr_pct") Double historicalFcfCagrPct,
        @JsonProperty("historical_period_span") String historicalPeriodSpan,
        @JsonProperty("is_growth_rate_clipped") Boolean isGrowthRateClipped,
        @JsonProperty("interpretation") String interpretation
) {}
