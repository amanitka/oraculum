package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EarningsEstimatesAgentOutput(@JsonProperty("summary") String summary) {
}
