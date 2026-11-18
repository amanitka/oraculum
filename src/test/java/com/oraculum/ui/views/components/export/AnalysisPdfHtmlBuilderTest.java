package com.oraculum.ui.views.components.export;

import com.oraculum.analyst.api.domain.AnalysisOutlook;
import com.oraculum.analyst.api.domain.ValuationAssessment;
import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionDriver;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionSummary;
import com.oraculum.analyst.api.dto.AnalysisResult.ImpliedEpsSnapshot;
import com.oraculum.analyst.api.dto.AnalysisResult.ScenarioCase;
import com.oraculum.analyst.api.dto.AnalysisResult.ScenariosResult;
import com.oraculum.analyst.api.dto.CompanyAnalysisViewDto;
import com.oraculum.ui.views.components.renderer.MarkdownRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisPdfHtmlBuilderTest {

    private AnalysisPdfHtmlBuilder htmlBuilder;

    @BeforeEach
    void setUp() {
        MarkdownRenderer markdownRenderer = new MarkdownRenderer(new JsonMapper());
        htmlBuilder = new AnalysisPdfHtmlBuilder(markdownRenderer, new AnalysisPdfSnapshotSectionBuilder(markdownRenderer));
    }

    @Test
    void buildXhtml_whenFullAnalysisProvided_rendersAllSectionsAndStaticCitations() {
        CompanyAnalysisViewDto dto = buildRichAnalysis();

        String xhtml = htmlBuilder.buildXhtml(dto, "body { font-family: Inter; }");

        assertThat(xhtml)
                .contains("Acme &amp; Co (ACME)")
                .contains("HIGH-CONVICTION ACCUMULATE")
                .contains("What Today's Price Requires")
                .contains("Key Bull Drivers")
                .contains("Valuation &amp; Decision Drivers")
                .contains("Scenarios &amp; Risk Framing")
                .contains("Executive Summary")
                .contains("Valuation Justification")
                .contains("Referenced Sources")
                .contains("<sup class=\"cite\"><a class=\"cite-link\" href=\"#cite-1\">1</a></sup>")
                .contains("id=\"cite-1\"")
                .contains("class=\"avoid-break\"")
                .doesNotContain("javascript:void(0)")
                .doesNotContain("\u26A0");
    }

    @Test
    void buildXhtml_whenLegacyAnalysisWithoutDecisionLayer_rendersGracefully() {
        AnalysisResult legacy = AnalysisResult.builder()
                .outlook(AnalysisOutlook.NEUTRAL)
                .valuation(ValuationAssessment.FAIRLY_VALUED)
                .conviction(3)
                .factorScores(Map.of("fundamental_health", 7.2, "valuation", 5.5))
                .executiveSummary("Legacy summary paragraph.")
                .build();
        CompanyAnalysisViewDto dto = new CompanyAnalysisViewDto();
        dto.setTicker("MSFT");
        dto.setAnalysisDate(LocalDate.of(2026, 5, 1));
        dto.setAnalysisResult(legacy);

        String xhtml = htmlBuilder.buildXhtml(dto, "");

        assertThat(xhtml)
                .contains("MSFT")
                .contains("HOLD / NEUTRAL")
                .contains("Legacy summary paragraph.")
                .doesNotContain("Referenced Sources")
                .doesNotContain("Scenarios &amp; Risk Framing");
    }

    private CompanyAnalysisViewDto buildRichAnalysis() {
        DecisionSummary summary = DecisionSummary.builder()
                .businessQuality("EXCEPTIONAL")
                .valuationVsExpectations("CONSERVATIVE")
                .methodAgreement("ALIGNED")
                .marketSetup("SUPPORTIVE_UPTREND")
                .keyValuationTension("DCF and multiples converge on modest upside.")
                .decisiveQuestion("Can operating margin stay above 28%?")
                .currentSharePrice(142.50f)
                .priceDate(LocalDate.of(2026, 11, 10))
                .nextCatalyst("Next Quarter End: 2026-12-31")
                .requiredFcfCagrPct(8.4f)
                .historicalFcfCagrPct(14.2f)
                .ttmFcfGrowthPct(16.0f)
                .targetFiscalYear("FY27")
                .impliedEpsByMultiple(List.of(new ImpliedEpsSnapshot(20.0f, "20x P/E", 7.12f, 8.20f, 15.2f)))
                .build();

        AnalysisResult result = AnalysisResult.builder()
                .outlook(AnalysisOutlook.BULLISH)
                .valuation(ValuationAssessment.UNDERVALUED)
                .conviction(4)
                .convictionReasons(List.of("High FCF conversion across cycles"))
                .factorScores(Map.of("fundamental_health", 8.5, "valuation", 7.8, "growth_prospects", 7.6, "risk_profile", 7.4))
                .thesis("Cash-generative leader priced below historical multiples [1].")
                .topBullPoints(List.of("Expanding free cash flow margin [1]"))
                .topBearPoints(List.of("Customer concentration risk"))
                .valuationOneLiner("Trades at 17x forward FCF.")
                .whatWouldChangeThis("Margin drops below 24%.")
                .decisionSummary(summary)
                .decisionDrivers(List.of(DecisionDriver.builder()
                        .factor("FCF Conversion")
                        .status("SUPPORTIVE")
                        .importance("HIGH")
                        .currentValue("108%")
                        .benchmarkOrRequired("> 90%")
                        .whatChangesView("Falls below 80% for two quarters")
                        .build()))
                .scenarios(new ScenariosResult(
                        new ScenarioCase("Accelerating cloud adoption", "15% revenue CAGR", "+35%", null, null),
                        new ScenarioCase("Steady compounder", "10% revenue CAGR", null, "+18%", null),
                        new ScenarioCase("Enterprise spending slowdown", "3% revenue CAGR", null, null, "-15%")
                ))
                .thesisBreakers(List.of("Revenue growth < 5% YoY"))
                .executiveSummary("## Overview\n\nStrong balance sheet & durable moat [1].")
                .recommendationReasoning("Intrinsic value supports accumulation [1 ?\u26A0\uFE0F].")
                .build();

        CompanyAnalysisViewDto dto = new CompanyAnalysisViewDto();
        dto.setTicker("ACME");
        dto.setCompanyName("Acme & Co");
        dto.setMarket("NASDAQ");
        dto.setSectorName("Technology");
        dto.setIndustryName("Software");
        dto.setAnalysisDate(LocalDate.of(2026, 11, 13));
        dto.setAnalysisResult(result);
        dto.setAnalysisData("{\"CITATIONS\":{\"1\":{\"_source\":\"SEC 10-K\",\"fcf\":1250000000}}}");
        return dto;
    }
}
