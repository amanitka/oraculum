package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record StandardAgentOutput(
        @JsonProperty("headline") String headline,
        @JsonProperty("score") double score,
        @JsonProperty("confidence") double confidence,
        @JsonProperty("confidence_reasons") List<String> confidenceReasons,
        @JsonProperty("strengths") List<String> strengths,
        @JsonProperty("weaknesses") List<String> weaknesses,
        @JsonProperty("evidence") List<AgentEvidence> evidence,
        @JsonProperty("metrics") Map<String, Object> metrics,
        @JsonProperty("summary") String summary
) {}
