package com.oraculum.company.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReverseDcfDto(
        @JsonProperty("current_market_cap") Float currentMarketCap,
        @JsonProperty("current_fcf") Float currentFcf,
        @JsonProperty("fcf_yield_pct") Float fcfYieldPct,
        @JsonProperty("discount_rate_pct") Float discountRatePct,
        @JsonProperty("projection_years") int projectionYears,
        @JsonProperty("terminal_growth_rate_pct") Float terminalGrowthRatePct,
        @JsonProperty("implied_fcf_growth_rate_pct") Float impliedFcfGrowthRatePct,
        @JsonProperty("historical_fcf_cagr_pct") Float historicalFcfCagrPct,
        @JsonProperty("interpretation") String interpretation
) {}
