package com.oraculum.analyst.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.analyst.api.domain.AnalysisOutlook;
import com.oraculum.analyst.api.domain.ValuationAssessment;

import java.util.List;
import java.util.Map;

/**
 * Structured output of a completed fundamental and valuation analysis.
 * <p>
 * Combines the full markdown report with an executive landing-page snapshot.
 * Stored as JSONB in {@code t_company_analysis.analysis_result}.
 */
public record AnalysisResult(

        // ── Full report ───────────────────────────────────────────────────────
        @JsonProperty("executive_summary")
        String executiveSummary,

        @JsonProperty("recommendation_reasoning")
        String recommendationReasoning,

        @JsonProperty("factor_scores")
        Map<String, Double> factorScores,

        @JsonProperty("key_drivers")
        List<String> keyDrivers,

        @JsonProperty("key_risks")
        List<String> keyRisks,

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
        String whatWouldChangeThis
) {
}
