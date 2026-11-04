package com.oraculum.analyst.service;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.analyst.dto.quality.DataQualityResult;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.domain.TickerDocumentSubtype;
import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.company.api.dto.ReverseDcfMatrixCellDto;
import com.oraculum.company.api.dto.ReverseDcfSensitivityMatrixDto;
import com.oraculum.company.api.dto.SharePriceSignalDto;
import com.oraculum.company.api.dto.TickerDocumentDto;
import com.oraculum.company.api.dto.ValuationZone;
import com.oraculum.economy.api.domain.MacroIndicator;
import com.oraculum.economy.api.dto.MacroSummaryDto;
import com.oraculum.harvester.api.dto.CompanyNewsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DataQualityMetricsCalculatorTest {

    private DataQualityMetricsCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DataQualityMetricsCalculator();
    }

    @Test
    void calculate_fundamentalsWithin120Days_returns100PercentFreshness() {
        LocalDate analysisDate = LocalDate.of(2026, 10, 15);
        LocalDate reportDate = LocalDate.of(2026, 7, 31);

        CompanyFinancialRatiosDto ratio = createRatio(reportDate);
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .companyFinancialRatios(Map.of(StatementVariant.TTM, List.of(ratio)))
                .build();

        AgentContext ctx = AgentContext.builder()
                .analysisDate(analysisDate)
                .factSheetData(factSheetData)
                .build();

        DataQualityResult result = calculator.calculate(AgentType.FUNDAMENTALS, ctx);

        assertThat(result.confidenceScore().data()).isEqualTo(1.0);
        assertThat(result.confidenceScore().overall()).isEqualTo(1.0);
        assertThat(result.confidenceReasons()).hasSize(3);
        assertThat(result.confidenceReasons().getFirst()).contains("100%");
        assertThat(result.confidenceReasons().get(1)).contains("100%").contains("current filing");
        assertThat(result.confidenceReasons().get(2)).contains("Interpretation confidence: 100%");
    }

    @Test
    void calculate_fundamentalsWithStaleDataOver120Days_reducesFreshness() {
        LocalDate analysisDate = LocalDate.of(2026, 10, 15);
        LocalDate reportDate = LocalDate.of(2025, 10, 15);

        CompanyFinancialRatiosDto ratio = createRatio(reportDate);
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .companyFinancialRatios(Map.of(StatementVariant.TTM, List.of(ratio)))
                .build();

        AgentContext ctx = AgentContext.builder()
                .analysisDate(analysisDate)
                .factSheetData(factSheetData)
                .build();

        DataQualityResult result = calculator.calculate(AgentType.FUNDAMENTALS, ctx);

        assertThat(result.confidenceScore().data()).isLessThan(0.15);
        assertThat(result.confidenceReasons().get(1)).contains("delayed filing");
    }

    @Test
    void calculate_newsWithArticles_returnsFullScore() {
        CompanyNewsDto newsDto = new CompanyNewsDto(1L, OffsetDateTime.now(), "headline", "summary", "source", "url", List.of());
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .recentNews(List.of(newsDto))
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(factSheetData)
                .build();

        DataQualityResult result = calculator.calculate(AgentType.NEWS, ctx);

        assertThat(result.confidenceScore().data()).isEqualTo(1.0);
        assertThat(result.confidenceReasons().get(1)).contains("recent news or SEC 8-K release available");
    }

    @Test
    void calculate_newsWithOnlySecEx991Release_returnsFullDataScore() {
        LocalDate analysisDate = LocalDate.of(2026, 10, 15);
        TickerDocumentDto doc = TickerDocumentDto.builder()
                .id("doc-1")
                .ticker("NVDA")
                .documentType(TickerDocumentType.SEC_8K)
                .documentSubtype(TickerDocumentSubtype.SEC_EX99_1)
                .filingDate(LocalDate.of(2026, 10, 1))
                .summary("{\"headline\":\"Q3 Earnings\"}")
                .build();
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .recentNews(List.of())
                .recentSecDocuments(Map.of(
                        TickerDocumentType.SEC_8K, Map.of(TickerDocumentSubtype.SEC_EX99_1, List.of(doc))
                ))
                .build();

        AgentContext ctx = AgentContext.builder()
                .analysisDate(analysisDate)
                .factSheetData(factSheetData)
                .build();

        DataQualityResult result = calculator.calculate(AgentType.NEWS, ctx);

        assertThat(result.confidenceScore().data()).isEqualTo(1.0);
        assertThat(result.confidenceScore().interpretation()).isEqualTo(0.95);
    }

    @Test
    void calculate_newsWithoutArticlesOrSecReleases_returnsZeroScore() {
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .recentNews(List.of())
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(factSheetData)
                .build();

        DataQualityResult result = calculator.calculate(AgentType.NEWS, ctx);

        assertThat(result.confidenceScore().data()).isEqualTo(0.0);
        assertThat(result.confidenceReasons().getFirst()).contains("no news articles found");
    }

    @Test
    void calculate_macroFiltersBySectorRelevance() {
        CompanyDto techCompany = new CompanyDto(
                1, "NVDA", "US", "Nvidia", "1", "Semiconductors", "Technology",
                "US1", "Desc", 1000L, "USD", "CIK", "CUSIP", null
        );
        // HOUST is Real Estate only, not in Technology's indicator set
        MacroSummaryDto irrelevantMacro = new MacroSummaryDto(
                MacroIndicator.HOUST, "Housing Starts", LocalDate.now(),
                1300.0, 1350.0, -3.7, -50.0, 1200.0, 1500.0, 1360.0, -1.0
        );
        MacroSummaryDto relevantMacro = new MacroSummaryDto(
                MacroIndicator.INDPRO, "Industrial Production", LocalDate.now(),
                100.0, 95.0, 5.0, 500.0, 90.0, 105.0, 98.0, 2.0
        );

        DataQualityResult noRelevant = calculator.calculate(
                AgentType.MACROECONOMIC,
                AgentContext.builder()
                        .factSheetData(CompanyFactSheetData.builder()
                                .company(techCompany)
                                .macroeconomicSummary(List.of(irrelevantMacro))
                                .build())
                        .build()
        );
        DataQualityResult withRelevant = calculator.calculate(
                AgentType.MACROECONOMIC,
                AgentContext.builder()
                        .factSheetData(CompanyFactSheetData.builder()
                                .company(techCompany)
                                .macroeconomicSummary(List.of(relevantMacro))
                                .build())
                        .build()
        );

        assertThat(noRelevant.confidenceScore().data()).isEqualTo(0.0);
        assertThat(withRelevant.confidenceScore().data()).isEqualTo(1.0);
    }

    @Test
    void calculate_sharePriceSignals_evaluatesCompletenessAndFreshness() {
        LocalDate analysisDate = LocalDate.of(2026, 10, 15);
        SharePriceSignalDto signal = SharePriceSignalDto.builder()
                .tradeDate(LocalDate.of(2026, 10, 14))
                .sharePrice(100.0f)
                .build();

        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .dailySharePriceSignals(List.of(signal))
                .build();

        AgentContext ctx = AgentContext.builder()
                .analysisDate(analysisDate)
                .factSheetData(factSheetData)
                .build();

        DataQualityResult result = calculator.calculate(AgentType.SHARE_PRICE, ctx);

        assertThat(result.confidenceScore().data()).isEqualTo(0.05);
    }

    @Test
    void calculate_valuationWithDcfAndSensitivity_returnsCompleteScore() {
        LocalDate analysisDate = LocalDate.of(2026, 10, 15);
        ReverseDcfSensitivityMatrixDto matrix = new ReverseDcfSensitivityMatrixDto(List.of(0.1), List.of(0.03), List.of(createCell()));
        ReverseDcfDto dcf = new ReverseDcfDto(100.0, 10.0, 0.1, 0.1, 10, 0.03, 12.0, 10.0, "5Y", false, "OK", matrix);

        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .reverseDcfResult(dcf)
                .companyFinancialRatios(Map.of(
                        StatementVariant.TTM, List.of(),
                        StatementVariant.ANNUAL, List.of()
                ))
                .build();

        AgentContext ctx = AgentContext.builder()
                .analysisDate(analysisDate)
                .factSheetData(factSheetData)
                .build();

        DataQualityResult result = calculator.calculate(AgentType.VALUATION, ctx);

        assertThat(result.confidenceScore().data()).isLessThan(0.70);
    }

    private CompanyFinancialRatiosDto createRatio(LocalDate reportDate) {
        return new CompanyFinancialRatiosDto(
                "id", 1, "AAPL", "USD", null, StatementVariant.TTM,
                2026, "Q3", reportDate, reportDate, reportDate,
                1000f, 800f, 500f, 400f, 100f, 0.2f, 0.25f, 0.15f,
                0.2f, 5000f, 1000f, 0.4f, 0.25f, 0.16f, 1.5f,
                1.2f, 0.5f, 5.0f, 10f, 45f, 30f, 0.1f, 0.12f, 0.15f,
                0.08f, 0.1f, 8, 1.1f, 1, 0, 1,
                4, 4, 4, 8.5f,
                null, null, null, null, null, 0, 0, 0
        );
    }

    private ReverseDcfMatrixCellDto createCell() {
        return new ReverseDcfMatrixCellDto(10.0, 3.0, 12.0, ValuationZone.FAIR, true);
    }
}
