package com.oraculum.analyst.dto.valuation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record ReverseDcfSummaryDto(
        @JsonProperty("base_case_implied_growth_pct") Float baseCaseImpliedGrowthPct,
        @JsonProperty("base_case_valuation_zone") String baseCaseValuationZone,
        @JsonProperty("min_implied_growth_pct") Float minImpliedGrowthPct,
        @JsonProperty("max_implied_growth_pct") Float maxImpliedGrowthPct,
        @JsonProperty("median_implied_growth_pct") Float medianImpliedGrowthPct,
        @JsonProperty("aggressive_or_speculative_cells_pct") Integer aggressiveOrSpeculativeCellsPct,
        @JsonProperty("historical_fcf_cagr_pct") Float historicalFcfCagrPct
) {
}
