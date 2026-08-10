package com.oraculum.analyst.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.analyst.api.domain.AnalysisOutlook;
import com.oraculum.analyst.api.domain.ValuationAssessment;

import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Structured output of a completed fundamental and valuation analysis.
 * <p>
 * Combines the full markdown report with an executive landing-page snapshot.
 * Stored as JSONB in {@code t_company_analysis.analysis_result}.
 */
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnalysisResult(

        // ── Full report ───────────────────────────────────────────────────────
        @JsonProperty("executive_summary")
        String executiveSummary,

        @JsonProperty("recommendation_reasoning")
        String recommendationReasoning,

        @JsonProperty("factor_scores")
        Map<String, Double> factorScores,

        // ── Valuation verdict (compliance-safe) ───────────────────────────────
        @JsonProperty("outlook")
        AnalysisOutlook outlook,

        @JsonProperty("valuation")
        ValuationAssessment valuation,

        @JsonProperty("conviction")
        int conviction,

        // ── Executive snapshot ───────────────────────────────────────────────
        @JsonProperty("thesis")
        String thesis,

        @JsonProperty("top_bull_points")
        List<String> topBullPoints,

        @JsonProperty("top_bear_points")
        List<String> topBearPoints,

        @JsonProperty("valuation_one_liner")
        String valuationOneLiner,

        @JsonProperty("what_would_change_this")
        String whatWouldChangeThis,

        @JsonProperty("thesis_breakers")
        List<String> thesisBreakers,

        @JsonProperty("scenarios")
        ScenariosResult scenarios
) {
    public AnalysisResult(
            String executiveSummary,
            String recommendationReasoning,
            Map<String, Double> factorScores,
            AnalysisOutlook outlook,
            ValuationAssessment valuation,
            int conviction,
            String thesis,
            List<String> topBullPoints,
            List<String> topBearPoints,
            String valuationOneLiner,
            String whatWouldChangeThis
    ) {
        this(executiveSummary, recommendationReasoning, factorScores, outlook, valuation, conviction,
             thesis, topBullPoints, topBearPoints, valuationOneLiner, whatWouldChangeThis, null, null);
    }

    public AnalysisResult withExecutiveSummary(String newExecutiveSummary) {
        return toBuilder().executiveSummary(newExecutiveSummary).build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScenariosResult(
            @JsonProperty("bull") ScenarioCase bull,
            @JsonProperty("base") ScenarioCase base,
            @JsonProperty("bear") ScenarioCase bear
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScenarioCase(
            @JsonProperty("description") String description,
            @JsonProperty("key_assumption") String keyAssumption,
            @JsonProperty("implied_upside") String impliedUpside,
            @JsonProperty("implied_move") String impliedMove,
            @JsonProperty("implied_downside") String impliedDownside
    ) {}
}
