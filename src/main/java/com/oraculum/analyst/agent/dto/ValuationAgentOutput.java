package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ValuationAgentOutput(@JsonProperty("multiple_analysis") String multipleAnalysis,
                                   @JsonProperty("dcf_perspective") String dcfPerspective,
                                   @JsonProperty("summary") String summary) {
}