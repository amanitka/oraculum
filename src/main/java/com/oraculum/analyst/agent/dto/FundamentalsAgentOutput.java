package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FundamentalsAgentOutput(@JsonProperty("growth_analysis") String growthAnalysis,
                                      @JsonProperty("profitability_analysis") String profitabilityAnalysis,
                                      @JsonProperty("quality_signals") String qualitySignals,
                                      @JsonProperty("summary") String summary) {
}