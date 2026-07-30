package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.oraculum.analyst.api.domain.AgentType;

import java.util.List;

public record CriticAgentOutput(
        @JsonProperty("contradictions_found")  List<Contradiction> contradictionsFound,
        @JsonProperty("is_consistent")         boolean isConsistent,
        @JsonProperty("recommended_reruns")    List<RerunInstruction> recommendedReruns
) {
    public record Contradiction(
            @JsonProperty("description")     String description,
            @JsonProperty("agents_involved") List<String> agentsInvolved,
            @JsonProperty("correction_type") CorrectionType correctionType,
            @JsonProperty("resolution")      Resolution resolution
    ) {}

    public enum CorrectionType {
        REASONING_ERROR,
        DATA_SOURCE_CONFLICT,
        TIME_WINDOW_MISMATCH,
        UNSUPPORTED_CLAIM,
        MISSING_EVIDENCE,
        DUPLICATE_LOGIC,
        OVERCONFIDENCE
    }

    public enum Resolution {
        RERUN_APPLIED,
        NARRATIVE_RECONCILED,
        UNRESOLVED
    }

    public record RerunInstruction(
            @JsonProperty("specialist")  AgentType specialist,
            @JsonProperty("severity")    int severity,
            @JsonProperty("instruction") String instruction
    ) {}

    /** Helper: contradictions that the Synthesizer needs to surface in the report. */
    public List<Contradiction> unresolvedContradictions() {
        if (contradictionsFound == null) return List.of();
        return contradictionsFound.stream()
                .filter(c -> c.resolution() == Resolution.UNRESOLVED)
                .toList();
    }
}
