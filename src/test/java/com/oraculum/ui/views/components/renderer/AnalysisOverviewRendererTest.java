package com.oraculum.ui.views.components.renderer;

import com.oraculum.analyst.api.domain.AnalysisOutlook;
import com.oraculum.analyst.api.domain.ValuationAssessment;
import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionDriver;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionSummary;
import com.vaadin.flow.component.Component;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class AnalysisOverviewRendererTest {

    @Test
    void normalizeFactorScores_scalesUpWhenAllScoresBelowOrEqualToOne() {
        Map<String, Double> input = Map.of(
                "fundamental_health", 0.78,
                "valuation", 0.79,
                "growth_prospects", 0.65,
                "risk_profile", 0.65
        );

        Map<String, Double> result = AnalysisOverviewRenderer.normalizeFactorScores(input);

        assertThat(result.get("fundamental_health")).isCloseTo(7.8, offset(0.001));
        assertThat(result.get("valuation")).isCloseTo(7.9, offset(0.001));
        assertThat(result.get("growth_prospects")).isCloseTo(6.5, offset(0.001));
        assertThat(result.get("risk_profile")).isCloseTo(6.5, offset(0.001));
    }

    @Test
    void normalizeFactorScores_preservesLowScoreWhenStandardScaleUsed() {
        Map<String, Double> input = Map.of(
                "fundamental_health", 8.5,
                "valuation", 0.8,
                "growth_prospects", 7.0,
                "risk_profile", 6.5
        );

        Map<String, Double> result = AnalysisOverviewRenderer.normalizeFactorScores(input);

        assertThat(result.get("fundamental_health")).isCloseTo(8.5, offset(0.001));
        assertThat(result.get("valuation")).isCloseTo(0.8, offset(0.001));
        assertThat(result.get("growth_prospects")).isCloseTo(7.0, offset(0.001));
        assertThat(result.get("risk_profile")).isCloseTo(6.5, offset(0.001));
    }

    @Test
    void normalizeFactorScores_scalesDownWhenHundredScaleUsed() {
        Map<String, Double> input = Map.of(
                "fundamental_health", 85.0,
                "valuation", 79.0,
                "growth_prospects", 65.0,
                "risk_profile", 65.0
        );

        Map<String, Double> result = AnalysisOverviewRenderer.normalizeFactorScores(input);

        assertThat(result.get("fundamental_health")).isCloseTo(8.5, offset(0.001));
        assertThat(result.get("valuation")).isCloseTo(7.9, offset(0.001));
        assertThat(result.get("growth_prospects")).isCloseTo(6.5, offset(0.001));
        assertThat(result.get("risk_profile")).isCloseTo(6.5, offset(0.001));
    }

    @Test
    void normalizeFactorScores_handlesNullAndEmptyMaps() {
        assertThat(AnalysisOverviewRenderer.normalizeFactorScores(null)).isEmpty();
        assertThat(AnalysisOverviewRenderer.normalizeFactorScores(Map.of())).isEmpty();
    }

    @Test
    void normalizeFactorScores_handlesNullValuesAndAllZeros() {
        Map<String, Double> input = new LinkedHashMap<>();
        input.put("fundamental_health", null);
        input.put("valuation", 0.0);

        Map<String, Double> result = AnalysisOverviewRenderer.normalizeFactorScores(input);

        assertThat(result.get("fundamental_health")).isEqualTo(0.0);
        assertThat(result.get("valuation")).isEqualTo(0.0);
    }

    @Test
    void renderOverviewTab_streamlinedExecutiveLayout_movesHeavyBlocksToDriversTab() {
        MarkdownRenderer markdownRenderer = new MarkdownRenderer(new JsonMapper());
        DecisionSnapshotRenderer decisionRenderer = new DecisionSnapshotRenderer();
        AnalysisOverviewRenderer renderer = new AnalysisOverviewRenderer(markdownRenderer, decisionRenderer);

        DecisionSummary summary = DecisionSummary.builder()
                .businessQuality("EXCEPTIONAL")
                .valuationVsExpectations("DEMANDING")
                .methodAgreement("CONFLICTING_PEAK_MARGIN_RISK")
                .marketSetup("PULLBACK_IN_UPTREND")
                .decisiveQuestion("Can FCF compound at 16.7%?")
                .keyValuationTension("Low P/E vs high DCF hurdle")
                .currentSharePrice(210.96f)
                .requiredFcfCagrPct(16.7f)
                .build();

        AnalysisResult result = AnalysisResult.builder()
                .valuation(ValuationAssessment.FAIRLY_VALUED)
                .outlook(AnalysisOutlook.BULLISH)
                .conviction(3)
                .thesis("Solid business model with high margins.")
                .factorScores(Map.of("fundamental_health", 9.0, "valuation", 6.4, "growth_prospects", 8.1, "risk_profile", 7.6))
                .topBullPoints(List.of("Strong AI capex"))
                .topBearPoints(List.of("Margin compression"))
                .valuationOneLiner("Trading at 26.7x P/E")
                .whatWouldChangeThis("Sustained slowdown below 40%")
                .decisionSummary(summary)
                .decisionDrivers(List.of(DecisionDriver.builder()
                        .factor("Implied FCF Growth Hurdle")
                        .status("DEMANDING")
                        .importance("HIGH")
                        .currentValue("76%")
                        .benchmarkOrRequired("16.7%")
                        .whatChangesView("Slowdown")
                        .build()))
                .convictionReasons(List.of("Capped at 3/5: valuation method conflict"))
                .build();

        AtomicBoolean openedDrivers = new AtomicBoolean(false);
        Component overview = renderer.renderOverviewTab(result, null, () -> openedDrivers.set(true));

        assertThat(overview).isNotNull();
        String overviewText = overview.getElement().getTextRecursively();
        assertThat(overviewText)
                .contains("HOLD / WATCHLIST")
                .contains("Business Outlook:")
                .contains("Bullish")
                .contains("Valuation Setup:")
                .contains("Fairly Valued")
                .contains("FUNDAMENTAL HEALTH")
                .contains("EXCEPTIONAL")
                .contains("VALUATION HURDLE")
                .contains("DEMANDING")
                .contains("Can FCF compound at 16.7%?")
                .contains("WHAT TODAY'S PRICE REQUIRES")
                .contains("Key Bull Drivers")
                .contains("Key Risks & Headwinds")
                .contains("Watch Trigger:")
                .contains("Valuation & Drivers")
                .doesNotContain("Decision Drivers & Monitoring Triggers")
                .doesNotContain("Conviction Audit & Active Guardrails");

        Component driversTab = renderer.renderValuationAndDriversTab(result, null);
        assertThat(driversTab).isNotNull();
        assertThat(driversTab.getElement().getTextRecursively())
                .contains("Valuation Context")
                .contains("Methodology Reconciliation")
                .contains("Decision Drivers & Monitoring Triggers")
                .contains("Implied FCF Growth Hurdle")
                .contains("Conviction Audit & Active Guardrails");
    }

    @Test
    void hasValuationAndDriversContent_checksActualRenderableFields() {
        MarkdownRenderer markdownRenderer = new MarkdownRenderer(new JsonMapper());
        AnalysisOverviewRenderer renderer = new AnalysisOverviewRenderer(markdownRenderer, new DecisionSnapshotRenderer());

        assertThat(renderer.hasValuationAndDriversContent(null)).isFalse();
        assertThat(renderer.hasValuationAndDriversContent(AnalysisResult.builder()
                .decisionSummary(DecisionSummary.builder().businessQuality("HIGH").build())
                .build())).isFalse();
        assertThat(renderer.hasValuationAndDriversContent(AnalysisResult.builder()
                .convictionReasons(List.of("Base conviction 4/5"))
                .build())).isTrue();
    }
}
