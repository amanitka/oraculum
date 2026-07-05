package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.oraculum.analyst.api.domain.AgentType;

import java.util.List;

public record CriticAgentOutput(
        @JsonProperty("contradictions_found") List<String> contradictionsFound,
        @JsonProperty("is_consistent") boolean isConsistent,
        @JsonProperty("recommended_reruns") List<RerunInstruction> recommendedReruns
) {
    public record RerunInstruction(
            @JsonProperty("specialist") AgentType specialist,
            @JsonProperty("severity") int severity,
            @JsonProperty("instruction") String instruction
    ) {}
}
