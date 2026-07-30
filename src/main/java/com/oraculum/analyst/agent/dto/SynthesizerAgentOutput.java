package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.analyst.api.domain.AnalysisOutlook;
import com.oraculum.analyst.api.domain.AnalysisRecommendation;

import java.util.List;
import java.util.Map;

public record SynthesizerAgentOutput(@JsonProperty("executive_summary") String executiveSummary,
                                     @JsonProperty("outlook") AnalysisOutlook outlook,
                                     @JsonProperty("recommendation") AnalysisRecommendation recommendation,
                                     @JsonProperty("conviction") int conviction,
                                     @JsonProperty("recommendation_reasoning") String recommendationReasoning,
                                     @JsonProperty("factor_scores") Map<String, Double> factorScores,
                                     @JsonProperty("key_drivers") List<String> keyDrivers,
                                     @JsonProperty("key_risks") List<String> keyRisks) {
}
