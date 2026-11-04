package com.oraculum.analyst.dto.quality;

public record InterpretationConfidenceResult(
        double score,
        String reason
) {
}
