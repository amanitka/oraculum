package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ValuationAgentOutput(@JsonProperty("multiple_analysis") String multipleAnalysis,
                                   @JsonProperty("summary") String summary) {
}