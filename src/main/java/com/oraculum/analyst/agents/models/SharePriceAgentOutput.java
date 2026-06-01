package com.oraculum.analyst.agents.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SharePriceAgentOutput(
    @JsonProperty("technical_analysis") String technicalAnalysis,
    @JsonProperty("momentum_analysis") String momentumAnalysis,
    @JsonProperty("summary") String summary
) {
}