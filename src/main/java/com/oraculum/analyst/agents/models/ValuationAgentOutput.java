package com.oraculum.analyst.agents.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ValuationAgentOutput(
    @JsonProperty("relative_valuation_analysis") String relativeValuationAnalysis,
    @JsonProperty("historical_valuation_analysis") String historicalValuationAnalysis,
    @JsonProperty("summary") String summary
) {
}