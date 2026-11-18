package com.oraculum.ui.views.components.export;

import com.oraculum.analyst.api.domain.AnalysisOutlook;
import com.oraculum.analyst.api.domain.ValuationAssessment;
import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionSummary;
import com.oraculum.analyst.api.dto.AnalysisResult.ImpliedEpsSnapshot;
import com.oraculum.analyst.api.dto.CompanyAnalysisViewDto;
import com.oraculum.ui.views.components.renderer.MarkdownRenderer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalysisPdfExportServiceTest {

    private AnalysisPdfExportService exportService;

    @BeforeEach
    void setUp() {
        MarkdownRenderer markdownRenderer = new MarkdownRenderer(new JsonMapper());
        AnalysisPdfHtmlBuilder htmlBuilder = new AnalysisPdfHtmlBuilder(
                markdownRenderer, new AnalysisPdfSnapshotSectionBuilder(markdownRenderer));
        exportService = new AnalysisPdfExportService(htmlBuilder);
    }

    @Test
    void exportPdf_whenValidAnalysis_producesReadablePdfWithClickableCitations() throws IOException {
        CompanyAnalysisViewDto dto = createValidDto();

        byte[] pdfBytes = exportService.exportPdf(dto);

        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, 0, 5)).isEqualTo("%PDF-");
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            String extracted = new PDFTextStripper().getText(doc);
            assertThat(extracted)
                    .contains("NVIDIA Corp (NVDA)")
                    .contains("Executive Summary")
                    .contains("Referenced Sources");
            assertThat(collectLinkAnnotations(doc)).isNotEmpty();
        }
    }

    @Test
    void exportPdf_whenBullBearDoesNotFitOnPageOne_keepsHeaderAndBulletsOnSamePage() throws IOException {
        CompanyAnalysisViewDto dto = createPageBreakStressDto();

        byte[] pdfBytes = exportService.exportPdf(dto);

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(2);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            String page1Text = stripper.getText(doc);
            stripper.setStartPage(2);
            stripper.setEndPage(2);
            String page2Text = stripper.getText(doc);

            boolean bullHeaderOnPage1 = page1Text.contains("KEY BULL DRIVERS");
            if (bullHeaderOnPage1) {
                assertThat(page1Text).contains("First major bull catalyst");
            } else {
                assertThat(page2Text).contains("KEY BULL DRIVERS").contains("First major bull catalyst");
            }
        }
    }

    private List<PDAnnotationLink> collectLinkAnnotations(PDDocument doc) throws IOException {
        List<PDAnnotationLink> links = new ArrayList<>();
        for (PDPage page : doc.getPages()) {
            for (PDAnnotation annotation : page.getAnnotations()) {
                if (annotation instanceof PDAnnotationLink link) {
                    links.add(link);
                }
            }
        }
        return links;
    }

    @Test
    void exportPdf_whenAnalysisMissingOrFailed_throwsException() {
        CompanyAnalysisViewDto failed = createValidDto();
        failed.setError("LLM timeout");

        assertThatThrownBy(() -> exportService.exportPdf(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> exportService.exportPdf(new CompanyAnalysisViewDto()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> exportService.exportPdf(failed))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void buildFileName_whenTickerAndDatePresent_returnsSanitizedPdfName() {
        CompanyAnalysisViewDto dto = new CompanyAnalysisViewDto();
        dto.setTicker("BRK.B");
        dto.setAnalysisDate(LocalDate.of(2026, 11, 13));

        assertThat(exportService.buildFileName(dto)).isEqualTo("BRK.B_analysis_2026-11-13.pdf");
        assertThat(exportService.buildFileName(new CompanyAnalysisViewDto())).isEqualTo("analysis_report.pdf");
    }

    private CompanyAnalysisViewDto createValidDto() {
        AnalysisResult result = AnalysisResult.builder()
                .outlook(AnalysisOutlook.BULLISH)
                .valuation(ValuationAssessment.FAIRLY_VALUED)
                .conviction(4)
                .factorScores(Map.of("fundamental_health", 9.0, "valuation", 6.2))
                .thesis("Dominant AI infrastructure ecosystem with strong pricing power [1].")
                .topBullPoints(List.of("Data center revenue compounding [1]"))
                .topBearPoints(List.of("Hyperscaler capex cyclicality"))
                .executiveSummary("Revenue and margins expanded sharply in recent quarters [1].")
                .recommendationReasoning("Current multiple fairly prices near-term growth.")
                .build();

        CompanyAnalysisViewDto dto = new CompanyAnalysisViewDto();
        dto.setTicker("NVDA");
        dto.setCompanyName("NVIDIA Corp");
        dto.setMarket("NASDAQ");
        dto.setAnalysisDate(LocalDate.of(2026, 11, 13));
        dto.setAnalysisResult(result);
        dto.setAnalysisData("{\"CITATIONS\":{\"1\":{\"_source\":\"SEC 10-Q\",\"revenue\":35100000000}}}");
        return dto;
    }

    private CompanyAnalysisViewDto createPageBreakStressDto() {
        DecisionSummary summary = DecisionSummary.builder()
                .businessQuality("EXCEPTIONAL")
                .valuationVsExpectations("DEMANDING")
                .methodAgreement("CONFLICTING")
                .marketSetup("PULLBACK_IN_UPTREND")
                .decisiveQuestion("Can consensus FCF compound at 16% through FY2035?")
                .currentSharePrice(200.13f)
                .priceDate(LocalDate.of(2026, 11, 4))
                .nextCatalyst("Next Quarter End: 2026-10-31")
                .requiredFcfCagrPct(16.0f)
                .historicalFcfCagrPct(83.1f)
                .ttmFcfGrowthPct(76.5f)
                .targetFiscalYear("2028-01-31")
                .impliedEpsByMultiple(List.of(
                        new ImpliedEpsSnapshot(13.4f, "Cyclical Compression", 14.94f, 15.91f, 6.5f),
                        new ImpliedEpsSnapshot(17.8f, "10Y Historical Min P/E", 11.24f, 15.91f, 41.6f),
                        new ImpliedEpsSnapshot(25.3f, "Current Anchor P/E", 7.91f, 15.91f, 101.2f)
                ))
                .build();

        String longThesis = "NVIDIA remains the dominant computing platform for global AI infrastructure, "
                + "supported by accelerating revenue growth and industry-leading profitability across data center "
                + "clusters, networking fabrics, and CUDA software ecosystems. While forward multiples look moderate, "
                + "the current share price implies substantial multi-year free cash flow compounding alongside "
                + "normalization of working capital absorption and sustained hyperscaler capital intensity.";

        AnalysisResult result = AnalysisResult.builder()
                .outlook(AnalysisOutlook.BULLISH)
                .valuation(ValuationAssessment.FAIRLY_VALUED)
                .conviction(3)
                .factorScores(Map.of("fundamental_health", 9.2, "valuation", 6.4, "growth_prospects", 7.9, "risk_profile", 7.8))
                .thesis(longThesis)
                .decisionSummary(summary)
                .topBullPoints(List.of(
                        "First major bull catalyst: quarterly revenue accelerated 105.9% YoY to $96.22B [1].",
                        "Second major bull catalyst: gross margin held at 75.0% well above industry peers [1].",
                        "Third major bull catalyst: raised forward guidance supports upward consensus EPS revisions."
                ))
                .topBearPoints(List.of(
                        "First risk factor: working capital absorbed $30.71B reducing quarterly FCF margin [1].",
                        "Second risk factor: single-stage reverse DCF embeds an aggressive 16.0% CAGR hurdle.",
                        "Third risk factor: export restrictions and higher Treasury yields elevate discount rates."
                ))
                .executiveSummary("Detailed executive summary paragraph [1].")
                .build();

        CompanyAnalysisViewDto dto = new CompanyAnalysisViewDto();
        dto.setTicker("NVDA");
        dto.setCompanyName("NVIDIA Corp");
        dto.setMarket("us");
        dto.setSectorName("Technology");
        dto.setIndustryName("Semiconductors");
        dto.setAnalysisDate(LocalDate.of(2026, 11, 6));
        dto.setAnalysisResult(result);
        dto.setAnalysisData("{\"CITATIONS\":{\"1\":{\"_source\":\"SEC 10-Q\",\"revenue\":96221000000}}}");
        return dto;
    }
}
