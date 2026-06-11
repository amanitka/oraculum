package com.oraculum.analyst.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.company.api.dto.SharePriceSignalDto;

public record AlgorithmicBaselineDto(
        @JsonProperty("composite_signal") String compositeSignal,
        @JsonProperty("quality_score") Float qualityScore
) {
    public static AlgorithmicBaselineDto from(SharePriceSignalDto latestSignal) {
        if (latestSignal == null) return null;
        return new AlgorithmicBaselineDto(
                latestSignal.compositeSignal(),
                latestSignal.qualityScore()
        );
    }
}
