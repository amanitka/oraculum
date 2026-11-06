package com.oraculum.analyst.dto.valuation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.company.api.dto.ValuationZone;
import lombok.Builder;

/**
 * Sensitivity row showing Two-Stage Reverse DCF implied growth across different FCF margin assumptions.
 */
@Builder
public record FcfMarginSensitivityEntryDto(
        @JsonProperty("scenario_label") String scenarioLabel,
        @JsonProperty("assumed_fcf_margin_pct") Float assumedFcfMarginPct,
        @JsonProperty("implied_fy2_fcf_usd") Double impliedFy2FcfUsd,
        @JsonProperty("implied_growth_years_3_to_10_pct") Float impliedGrowthYears3To10Pct,
        @JsonProperty("valuation_zone") ValuationZone valuationZone
) {
}
