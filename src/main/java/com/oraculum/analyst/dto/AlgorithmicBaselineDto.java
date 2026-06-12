package com.oraculum.analyst.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record AlgorithmicBaselineDto(
        @JsonProperty("timeframes") Map<String, TimeframeScores> timeframes
) {
    public record TimeframeScores(
            @JsonProperty("quality_score") Float qualityScore,
            @JsonProperty("piotroski_f_score") Integer piotroskiFScore
    ) {
    }
}
