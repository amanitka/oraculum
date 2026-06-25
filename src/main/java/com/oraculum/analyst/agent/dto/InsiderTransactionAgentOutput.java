package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.oraculum.analyst.api.domain.InsiderSentiment;

import java.util.List;

public record InsiderTransactionAgentOutput(
        @JsonProperty("management_sentiment") InsiderSentiment managementSentiment,
        @JsonProperty("bullish_conviction") int bullishConviction,
        @JsonProperty("key_signals") List<String> keySignals,
        @JsonProperty("cluster_buy_analysis") String clusterBuyAnalysis,
        @JsonProperty("summary") String summary
) {}
