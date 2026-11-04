package com.oraculum.analyst.dto.valuation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record SensitivityCellDto(
        @JsonProperty("multiple") Float multiple,
        @JsonProperty("eps_scenario") String epsScenario,
        @JsonProperty("eps_value") Float epsValue,
        @JsonProperty("implied_price") Float impliedPrice,
        @JsonProperty("pct_move_vs_current") Float pctMoveVsCurrent,
        @JsonProperty("discounted_implied_price") Float discountedImpliedPrice,
        @JsonProperty("discounted_pct_move_vs_current") Float discountedPctMoveVsCurrent
) {
}
