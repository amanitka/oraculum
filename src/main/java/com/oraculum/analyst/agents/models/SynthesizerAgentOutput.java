package com.oraculum.analyst.agents.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SynthesizerAgentOutput(
    @JsonProperty("executive_summary") String executiveSummary,
    @JsonProperty("verdict") Verdict verdict
) {
}