package com.oraculum.ui.views.components.renderer;

import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionDriver;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionSummary;
import com.oraculum.analyst.api.dto.AnalysisResult.ImpliedEpsSnapshot;
import com.vaadin.flow.component.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DecisionSnapshotRendererTest {

    private DecisionSnapshotRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new DecisionSnapshotRenderer();
    }

    @Test
    void renderDecisionSupportSection_nullOrEmptySnapshot_returnsNull() {
        assertThat(renderer.renderDecisionSupportSection(null)).isNull();
        assertThat(renderer.renderDecisionSupportSection(AnalysisResult.builder().build())).isNull();
    }

    @Test
    void renderDecisionSupportSection_withDecisionSummaryAndDrivers_rendersComponents() {
        DecisionSummary summary = DecisionSummary.builder()
                .businessQuality("EXCEPTIONAL")
                .valuationVsExpectations("DEMANDING")
                .methodAgreement("CONFLICTING_PEAK_MARGIN_RISK")
                .marketSetup("PULLBACK_IN_UPTREND")
                .keyValuationTension("5th percentile P/E vs 16.7% 10Y FCF CAGR required")
                .decisiveQuestion("Can FCF compound at ≥16.7% annually over 10 years?")
                .currentSharePrice(210.96f)
                .priceDate(LocalDate.of(2026, 10, 23))
                .priceStalenessDays(5)
                .nextCatalyst("Next Quarter End: 2026-10-31")
                .requiredFcfCagrPct(16.68f)
                .historicalFcfCagrPct(83.1f)
                .ttmFcfGrowthPct(76.5f)
                .targetFiscalYear("2028-01-31")
                .impliedEpsByMultiple(List.of(
                        new ImpliedEpsSnapshot(25.0f, "10Y Historical Min P/E (25.0x)", 8.44f, 15.91f, 88.5f)
                ))
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
                .decisionSummary(summary)
                .decisionDrivers(List.of(driver))
                .convictionReasons(List.of("Capped at 3/5: Valuation method conflict"))
                .build();

        Component rendered = renderer.renderDecisionSupportSection(result);

        assertThat(rendered).isNotNull();
        assertThat(rendered.getElement().getTextRecursively())
                .contains("EXCEPTIONAL")
                .contains("DEMANDING")
                .contains("Can FCF compound at ≥16.7% annually over 10 years?")
                .contains("Implied FCF Growth Hurdle");
    }
}
