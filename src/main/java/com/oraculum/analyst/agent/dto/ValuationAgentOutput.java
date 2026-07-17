package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ValuationAgentOutput(@JsonProperty("multiple_analysis") String multipleAnalysis,
                                   @JsonProperty("intrinsic_value_assessment") String intrinsicValueAssessment,
                                   @JsonProperty("summary") String summary) {
}
