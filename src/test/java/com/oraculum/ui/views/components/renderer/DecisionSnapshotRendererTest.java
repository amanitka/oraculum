package com.oraculum.ui.views.components.renderer;

import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionDriver;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionSummary;
import com.oraculum.analyst.api.dto.AnalysisResult.ImpliedEpsSnapshot;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionSnapshotRendererTest {

    private DecisionSnapshotRenderer renderer;
    private MarkdownRenderer markdownRenderer;

    @BeforeEach
    void setUp() {
        renderer = new DecisionSnapshotRenderer();
        markdownRenderer = new MarkdownRenderer(new JsonMapper());
    }

    @Test
    void renderDecisionSupportSection_nullOrEmptySnapshot_returnsNull() {
        assertThat(renderer.renderDecisionSupportSection(null)).isNull();
        assertThat(renderer.renderDecisionSupportSection(AnalysisResult.builder().build())).isNull();
    }

    @Test
    void renderUnifiedPillarsGrid_rendersFourSleekPillarsWithoutOvalPillTheme() {
        DecisionSummary summary = DecisionSummary.builder()
                .businessQuality("EXCEPTIONAL")
                .valuationVsExpectations("SPECULATIVE")
                .marketSetup("PULLBACK_IN_UPTREND")
                .ttmFcfGrowthPct(76.5f)
                .build();

        AnalysisResult snapshot = AnalysisResult.builder()
                .factorScores(Map.of(
                        "fundamental_health", 9.0,
                        "valuation", 6.4,
                        "growth_prospects", 8.1,
                        "risk_profile", 7.6
                ))
                .decisionSummary(summary)
                .build();

        Component grid = renderer.renderUnifiedPillarsGrid(snapshot, summary);

        assertThat(grid).isNotNull();
        String text = grid.getElement().getTextRecursively();
        assertThat(text)
                .contains("FUNDAMENTAL HEALTH", "9.0", "EXCEPTIONAL")
                .contains("VALUATION HURDLE", "6.4", "SPECULATIVE")
                .contains("GROWTH PROSPECTS", "8.1", "HIGH EXPANSION")
                .contains("RISK & MARKET SETUP", "7.6", "PULLBACK IN UPTREND");

        assertNoPillThemeRecursively(grid.getElement());
    }

    @Test
    void renderImpliedExpectationsCard_rendersUnifiedDcfAndHorizontalPeScale() {
        DecisionSummary summary = DecisionSummary.builder()
                .methodAgreement("CONFLICTING_PEAK_MARGIN_RISK")
                .requiredFcfCagrPct(16.68f)
                .ttmFcfGrowthPct(76.5f)
                .historicalFcfCagrPct(83.1f)
                .targetFiscalYear("2028-01-31")
                .impliedEpsByMultiple(List.of(
                        new ImpliedEpsSnapshot(13.4f, "Cyclical Compression (13.4x, 0.75x 10Y Min)", 15.74f, 15.91f, 1.1f),
                        new ImpliedEpsSnapshot(17.8f, "10Y Historical Min P/E (17.8x)", 11.85f, 15.91f, 34.3f),
                        new ImpliedEpsSnapshot(26.7f, "Current / Historical Anchor P/E (26.7x)", 7.90f, 15.91f, 101.5f)
                ))
                .build();

        Component card = renderer.renderImpliedExpectationsCard(summary);

        assertThat(card).isNotNull();
        String text = card.getElement().getTextRecursively();
        assertThat(text)
                .contains("WHAT TODAY'S PRICE REQUIRES")
                .contains("Method Conflict: CONFLICTING PEAK MARGIN RISK")
                .contains("16.7%", "FCF CAGR")
                .contains("Recent TTM: 76.5%", "5Y CAGR: 83.1%")
                .contains("Consensus EPS: $15.91")
                .contains("13.4x • Cyclical Compression")
                .contains("+34.3% buffer");

        assertNoPillThemeRecursively(card.getElement());
    }

    @Test
    void renderValuationAndDriversTab_rendersContextMethodologyDriversAndAudit() {
        DecisionSummary summary = DecisionSummary.builder()
                .methodAgreement("CONFLICTING_PEAK_MARGIN_RISK")
                .keyValuationTension("5th percentile P/E vs 16.7% 10Y FCF CAGR required")
                .build();

        DecisionDriver driver = DecisionDriver.builder()
                .factor("Implied FCF Growth Hurdle")
                .status("DEMANDING")
                .importance("CRITICAL")
                .currentValue("76.5% TTM YoY")
                .benchmarkOrRequired("16.7% 10Y CAGR")
                .whatChangesView("FCF growth < 16.7% for 2 quarters")
                .build();

        AnalysisResult result = AnalysisResult.builder()
                .conviction(3)
                .valuationOneLiner("At $210.96, NVDA trades at 26.7x trailing P/E.")
                .decisionSummary(summary)
                .decisionDrivers(List.of(driver))
                .convictionReasons(List.of("Capped at 3/5: Valuation method conflict"))
                .build();

        Component tab = renderer.renderValuationAndDriversTab(result, null, markdownRenderer);

        assertThat(tab).isNotNull();
        String text = tab.getElement().getTextRecursively();
        assertThat(text)
                .contains("Valuation Context")
                .contains("Methodology Reconciliation")
                .contains("5th percentile P/E vs 16.7% 10Y FCF CAGR required")
                .contains("Decision Drivers & Monitoring Triggers")
                .contains("Implied FCF Growth Hurdle")
                .contains("Conviction Audit & Active Guardrails (Final Conviction 3/5)")
                .contains("Capped at 3/5: Valuation method conflict");

        assertNoPillThemeRecursively(tab.getElement());
    }

    private void assertNoPillThemeRecursively(Element element) {
        assertThat(element.getThemeList()).doesNotContain("pill");
        element.getChildren().forEach(this::assertNoPillThemeRecursively);
    }
}
