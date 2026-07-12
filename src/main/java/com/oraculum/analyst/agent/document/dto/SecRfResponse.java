package com.oraculum.analyst.agent.document.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SecRfResponse(
        @JsonProperty("summary") String summary,
        @JsonProperty("material_risks") List<MaterialRisk> materialRisks,
        @JsonProperty("risk_level") String riskLevel,
        @JsonProperty("new_risks") List<String> newRisks,
        @JsonProperty("mitigations") List<String> mitigations,
        @JsonProperty("sentiment_score") Double sentimentScore
) {
    public record MaterialRisk(
            @JsonProperty("risk") String risk,
            @JsonProperty("category") String category,
            @JsonProperty("severity") String severity
    ) {
    }
}
