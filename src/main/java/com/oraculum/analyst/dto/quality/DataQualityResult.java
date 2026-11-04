package com.oraculum.analyst.dto.quality;

import com.oraculum.analyst.agent.dto.StandardAgentOutput.ConfidenceScore;

import java.util.List;

public record DataQualityResult(
        ConfidenceScore confidenceScore,
        List<String> confidenceReasons
) {
}
