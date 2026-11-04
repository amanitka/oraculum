package com.oraculum.analyst.dto.valuation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record YearFcfProxyResult(
        @JsonProperty("fcf_proxy") Double fcfProxy,
        @JsonProperty("fcf_path_a") Double fcfPathA,
        @JsonProperty("fcf_path_b") Double fcfPathB,
        @JsonProperty("divergence_pct") Float divergencePct,
        @JsonProperty("is_divergent") boolean isDivergent
) {
}
