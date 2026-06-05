package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SharePriceAgentOutput(@JsonProperty("momentum_analysis") String momentumAnalysis,
                                    @JsonProperty("valuation_analysis") String valuationAnalysis,
                                    @JsonProperty("historical_trend_analysis") String historicalTrendAnalysis,
                                    @JsonProperty("key_signals_summary") String keySignalsSummary) {
}