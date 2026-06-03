package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AnalysisInsight(@JsonProperty("insight_summary") String insightSummary,
                              @JsonProperty("supporting_data_points") List<String> supportingDataPoints) {
}