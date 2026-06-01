package com.oraculum.analyst.agents.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.analyst.domain.AnalysisRecommendation;

public record Verdict(
    @JsonProperty("recommendation") AnalysisRecommendation recommendation,
    @JsonProperty("confidence_score") double confidenceScore,
    @JsonProperty("summary") String summary
) {
}