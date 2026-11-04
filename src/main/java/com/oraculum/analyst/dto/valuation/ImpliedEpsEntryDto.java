package com.oraculum.analyst.dto.valuation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record ImpliedEpsEntryDto(
        @JsonProperty("multiple") Float multiple,
        @JsonProperty("anchor_label") String anchorLabel,
        @JsonProperty("required_eps_for_current_price") Float requiredEpsForCurrentPrice,
        @JsonProperty("consensus_eps_avg") Float consensusEpsAvg,
        @JsonProperty("consensus_vs_required_pct") Float consensusVsRequiredPct
) {
}
