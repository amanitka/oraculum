package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NewsAgentOutput(@JsonProperty("summary") String summary) {
}
