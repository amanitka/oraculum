package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.analyst.agent.dto.StandardAgentOutput.ConfidenceScore;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record RiskAgentOutput(
        @JsonProperty("headline") String headline,
        @JsonProperty("score") double score,
        @JsonProperty("confidence") ConfidenceScore confidence,
        @JsonProperty("confidence_reasons") List<String> confidenceReasons,
        @JsonProperty("business_risks") List<String> businessRisks,
        @JsonProperty("financial_risks") List<String> financialRisks,
        @JsonProperty("macro_risks") List<String> macroRisks,
        @JsonProperty("sentiment_signals") List<String> sentimentSignals,
        @JsonProperty("evidence") List<AgentEvidence> evidence,
        @JsonProperty("metrics") Map<String, Object> metrics,
        @JsonProperty("summary") String summary
) implements SpecialistOutput {
    @Override
    public RiskAgentOutput withConfidence(ConfidenceScore confidence, List<String> reasons) {
        return this.toBuilder()
                .confidence(confidence)
                .confidenceReasons(reasons)
                .build();
    }

    public static RiskAgentOutput empty(String message) {
        return empty(message, message);
    }

    public static RiskAgentOutput empty(String headline, String summary) {
        return RiskAgentOutput.builder()
                .headline(headline)
                .score(0.0)
                .confidence(new ConfidenceScore(0.0, 0.0, 0.0))
                .confidenceReasons(List.of())
                .businessRisks(List.of())
                .financialRisks(List.of())
                .macroRisks(List.of())
                .sentimentSignals(List.of())
                .evidence(List.of())
                .metrics(Map.of())
                .summary(summary)
                .build();
    }
}
