package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record StandardAgentOutput(
        @JsonProperty("headline") String headline,
        @JsonProperty("score") double score,
        @JsonProperty("confidence") ConfidenceScore confidence,
        @JsonProperty("confidence_reasons") List<String> confidenceReasons,
        @JsonProperty("strengths") List<String> strengths,
        @JsonProperty("weaknesses") List<String> weaknesses,
        @JsonProperty("evidence") List<AgentEvidence> evidence,
        @JsonProperty("metrics") Map<String, Object> metrics,
        @JsonProperty("summary") String summary
) implements SpecialistOutput {
    @JsonIgnore
    public StandardAgentOutput(String headline, double score, double confidence, List<String> confidenceReasons, List<String> strengths, List<String> weaknesses, List<AgentEvidence> evidence, Map<String, Object> metrics, String summary) {
        this(headline, score, new ConfidenceScore(null, null, confidence), confidenceReasons, strengths, weaknesses, evidence, metrics, summary);
    }

    public static StandardAgentOutput empty(String message) {
        return empty(message, message);
    }

    public static StandardAgentOutput empty(String headline, String summary) {
        return StandardAgentOutput.builder()
                .headline(headline)
                .score(0.0)
                .confidence(new ConfidenceScore(0.0, 0.0, 0.0))
                .confidenceReasons(List.of())
                .strengths(List.of())
                .weaknesses(List.of())
                .evidence(List.of())
                .metrics(Map.of())
                .summary(summary)
                .build();
    }

    @Override
    public StandardAgentOutput withConfidence(ConfidenceScore confidence, List<String> reasons) {
        return this.toBuilder()
                .confidence(confidence)
                .confidenceReasons(reasons)
                .build();
    }

    public record ConfidenceScore(Double data, Double interpretation, Double overall) {
    }
}
