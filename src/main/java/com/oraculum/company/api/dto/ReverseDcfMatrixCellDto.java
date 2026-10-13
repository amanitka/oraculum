package com.oraculum.company.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReverseDcfMatrixCellDto(
        @JsonProperty("discount_rate_pct") double discountRatePct,
        @JsonProperty("terminal_growth_rate_pct") double terminalGrowthRatePct,
        @JsonProperty("implied_fcf_growth_pct") double impliedFcfGrowthPct,
        @JsonProperty("valuation_zone") ValuationZone valuationZone,
        @JsonProperty("is_base_case") boolean isBaseCase
) {}
