package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MetricEvidence(
        @JsonProperty("metric") String metric,
        @JsonProperty("value") String value,
        @JsonProperty("benchmark") String benchmark
) {}
