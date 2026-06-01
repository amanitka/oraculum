package com.oraculum.analyst.agents.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NewsAgentOutput(
    @JsonProperty("summary") String summary
) {
}