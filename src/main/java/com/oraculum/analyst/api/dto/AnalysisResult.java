package com.oraculum.analyst.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.analyst.api.domain.AnalysisOutlook;
import com.oraculum.analyst.api.domain.ValuationAssessment;
import lombok.Builder;

import java.time.LocalDate;
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

        @JsonProperty("conviction_reasons")
        List<String> convictionReasons,

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
        ScenariosResult scenarios,

        // ── Deterministic decision support layer ──────────────────────────────
        @JsonProperty("decision_summary")
        DecisionSummary decisionSummary,

        @JsonProperty("decision_drivers")
        List<DecisionDriver> decisionDrivers
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
             null, thesis, topBullPoints, topBearPoints, valuationOneLiner, whatWouldChangeThis, null, null, null, null);
    }

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
            String whatWouldChangeThis,
            List<String> thesisBreakers,
            ScenariosResult scenarios
    ) {
        this(executiveSummary, recommendationReasoning, factorScores, outlook, valuation, conviction,
             null, thesis, topBullPoints, topBearPoints, valuationOneLiner, whatWouldChangeThis,
             thesisBreakers, scenarios, null, null);
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

    @Builder(toBuilder = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DecisionSummary(
            @JsonProperty("business_quality") String businessQuality,
            @JsonProperty("valuation_vs_expectations") String valuationVsExpectations,
            @JsonProperty("method_agreement") String methodAgreement,
            @JsonProperty("market_setup") String marketSetup,
            @JsonProperty("key_valuation_tension") String keyValuationTension,
            @JsonProperty("decisive_question") String decisiveQuestion,
            @JsonProperty("current_share_price") Float currentSharePrice,
            @JsonProperty("price_date") LocalDate priceDate,
            @JsonProperty("price_staleness_days") Integer priceStalenessDays,
            @JsonProperty("next_catalyst") String nextCatalyst,
            @JsonProperty("required_fcf_cagr_pct") Float requiredFcfCagrPct,
            @JsonProperty("historical_fcf_cagr_pct") Float historicalFcfCagrPct,
            @JsonProperty("ttm_fcf_growth_pct") Float ttmFcfGrowthPct,
            @JsonProperty("target_fiscal_year") String targetFiscalYear,
            @JsonProperty("implied_eps_by_multiple") List<ImpliedEpsSnapshot> impliedEpsByMultiple
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImpliedEpsSnapshot(
            @JsonProperty("multiple") Float multiple,
            @JsonProperty("anchor_label") String anchorLabel,
            @JsonProperty("required_eps_for_current_price") Float requiredEpsForCurrentPrice,
            @JsonProperty("consensus_eps_avg") Float consensusEpsAvg,
            @JsonProperty("consensus_vs_required_pct") Float consensusVsRequiredPct
    ) {}

    @Builder(toBuilder = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DecisionDriver(
            @JsonProperty("factor") String factor,
            @JsonProperty("status") String status,
            @JsonProperty("importance") String importance,
            @JsonProperty("current_value") String currentValue,
            @JsonProperty("benchmark_or_required") String benchmarkOrRequired,
            @JsonProperty("what_changes_view") String whatChangesView
    ) {}
}
