package com.oraculum.analyst.agents.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CriticAgentOutput(
    @JsonProperty("contradictions_found") List<String> contradictionsFound,
    @JsonProperty("is_consistent") boolean isConsistent
) {
}