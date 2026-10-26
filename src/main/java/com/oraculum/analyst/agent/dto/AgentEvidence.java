package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AgentEvidence(
        @JsonProperty("metric") String metric,
        @JsonProperty("value") String value,
        @JsonProperty("previous_value") String previousValue,
        @JsonProperty("trend") String trend,
        @JsonProperty("source_type") String sourceType,
        @JsonProperty("significance") String significance
) {
    public AgentEvidence(String metric, String value, String previousValue, String trend, String significance) {
        this(metric, value, previousValue, trend, null, significance);
    }
}
