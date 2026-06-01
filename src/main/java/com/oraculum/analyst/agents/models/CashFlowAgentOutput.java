package com.oraculum.analyst.agents.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CashFlowAgentOutput(
        @JsonProperty("cash_generation_analysis") String cashGenerationAnalysis,
        @JsonProperty("capex_intensity_analysis") String capexIntensityAnalysis,
        @JsonProperty("summary") String summary
) {
}