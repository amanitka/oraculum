package com.oraculum.analyst.dto.valuation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record MatrixSummaryDto(
        @JsonProperty("min_pct_move") Float minPctMove,
        @JsonProperty("max_pct_move") Float maxPctMove,
        @JsonProperty("median_pct_move") Float medianPctMove,
        @JsonProperty("base_case_multiple") Float baseCaseMultiple,
        @JsonProperty("base_case_implied_price") Float baseCaseImpliedPrice,
        @JsonProperty("base_case_pct_move") Float baseCasePctMove,
        @JsonProperty("discounted_min_pct_move") Float discountedMinPctMove,
        @JsonProperty("discounted_max_pct_move") Float discountedMaxPctMove,
        @JsonProperty("discounted_median_pct_move") Float discountedMedianPctMove,
        @JsonProperty("discounted_base_case_implied_price") Float discountedBaseCaseImpliedPrice,
        @JsonProperty("discounted_base_case_pct_move") Float discountedBaseCasePctMove,
        @JsonProperty("break_even_pe_on_avg_eps") Float breakEvenPeOnAvgEps
) {
}
