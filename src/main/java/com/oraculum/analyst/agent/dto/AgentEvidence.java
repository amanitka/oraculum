package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AgentEvidence(
        @JsonProperty("claim") String claim,
        @JsonProperty("metrics") List<MetricEvidence> metrics
) {}
