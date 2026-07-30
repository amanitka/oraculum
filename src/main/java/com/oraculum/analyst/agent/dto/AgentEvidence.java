package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AgentEvidence(
        @JsonProperty("metric") String metric,
        @JsonProperty("value") String value,
        @JsonProperty("previous_value") String previousValue,
        @JsonProperty("trend") String trend,
        @JsonProperty("significance") String significance
) {}
