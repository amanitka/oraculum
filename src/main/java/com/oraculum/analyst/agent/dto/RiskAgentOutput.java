package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RiskAgentOutput(@JsonProperty("red_flags") List<String> redFlags,
                              @JsonProperty("solvency_analysis") String solvencyAnalysis,
                              @JsonProperty("summary") String summary) {
}