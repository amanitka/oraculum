package com.oraculum.analyst.service;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.*;
import com.oraculum.economy.api.dto.MacroSummaryDto;
import com.oraculum.harvester.api.dto.CompanyNewsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataQualityMetricsCalculatorTest {

    private DataQualityMetricsCalculator calculator;

    @Mock
    private CompanyFactSheetData factSheetData;

    @BeforeEach
    void setUp() {
        calculator = new DataQualityMetricsCalculator();
    }

    @Test
    void calculate_fundamentalsWithin120Days_returns100PercentFreshness() {
        LocalDate analysisDate = LocalDate.of(2026, 10, 15);
        LocalDate reportDate = LocalDate.of(2026, 7, 31); // 76 days ago (NVDA scenario)

        CompanyFinancialRatiosDto ratio = mockRatio(reportDate);
        when(factSheetData.getCompanyFinancialRatiosList(StatementVariant.TTM)).thenReturn(List.of(ratio));

        AgentContext ctx = AgentContext.builder()
                .analysisDate(analysisDate)
                .factSheetData(factSheetData)
                .build();

        var result = calculator.calculate(AgentType.FUNDAMENTALS, ctx);

        assertThat(result.confidenceScore().data()).isEqualTo(1.0);
        assertThat(result.confidenceScore().overall()).isEqualTo(1.0);
        assertThat(result.confidenceReasons()).hasSize(2);
        assertThat(result.confidenceReasons().getFirst()).contains("100%");
        assertThat(result.confidenceReasons().get(1)).contains("100%").contains("current filing");
    }

    @Test
    void calculate_fundamentalsWithStaleDataOver120Days_reducesFreshness() {
        LocalDate analysisDate = LocalDate.of(2026, 10, 15);
        LocalDate reportDate = LocalDate.of(2025, 10, 15); // 365 days ago

        CompanyFinancialRatiosDto ratio = mockRatio(reportDate);
        when(factSheetData.getCompanyFinancialRatiosList(StatementVariant.TTM)).thenReturn(List.of(ratio));

        AgentContext ctx = AgentContext.builder()
                .analysisDate(analysisDate)
                .factSheetData(factSheetData)
                .build();

        var result = calculator.calculate(AgentType.FUNDAMENTALS, ctx);

        assertThat(result.confidenceScore().data()).isLessThan(0.15);
        assertThat(result.confidenceReasons().get(1)).contains("delayed filing");
    }

    @Test
    void calculate_newsWithArticles_returnsFullScore() {
        CompanyNewsDto newsDto = mock(CompanyNewsDto.class);
        when(factSheetData.getRecentNewsList()).thenReturn(List.of(newsDto));

        AgentContext ctx = AgentContext.builder()
                .factSheetData(factSheetData)
                .build();

        var result = calculator.calculate(AgentType.NEWS, ctx);

        assertThat(result.confidenceScore().data()).isEqualTo(1.0);
        assertThat(result.confidenceReasons().get(1)).contains("recent news available");
    }

    @Test
    void calculate_newsWithoutArticles_returnsZeroScore() {
        when(factSheetData.getRecentNewsList()).thenReturn(List.of());

        AgentContext ctx = AgentContext.builder()
                .factSheetData(factSheetData)
                .build();

        var result = calculator.calculate(AgentType.NEWS, ctx);

        assertThat(result.confidenceScore().data()).isEqualTo(0.0);
        assertThat(result.confidenceReasons().getFirst()).contains("no news articles found");
    }

    @Test
    void calculate_macroWithSummary_returnsFullScore() {
        MacroSummaryDto macroDto = mock(MacroSummaryDto.class);
        when(factSheetData.getMacroeconomicSummaryList()).thenReturn(List.of(macroDto));

        AgentContext ctx = AgentContext.builder()
                .factSheetData(factSheetData)
                .build();

        var result = calculator.calculate(AgentType.MACROECONOMIC, ctx);

        assertThat(result.confidenceScore().data()).isEqualTo(1.0);
        assertThat(result.confidenceReasons().get(1)).contains("current economic observation series");
    }

    @Test
    void calculate_sharePriceSignals_evaluatesCompletenessAndFreshness() {
        LocalDate analysisDate = LocalDate.of(2026, 10, 15);
        SharePriceSignalDto signal = mock(SharePriceSignalDto.class);
        when(signal.tradeDate()).thenReturn(LocalDate.of(2026, 10, 14)); // 1 day ago (within 3d buffer)

        when(factSheetData.getDailySharePriceSignalsList()).thenReturn(List.of(signal));
        when(factSheetData.getLatestDailySignal()).thenReturn(signal);

        AgentContext ctx = AgentContext.builder()
                .analysisDate(analysisDate)
                .factSheetData(factSheetData)
                .build();

        var result = calculator.calculate(AgentType.SHARE_PRICE, ctx);

        assertThat(result.confidenceScore().data()).isEqualTo(0.05); // 1 / 20 completeness * 1.0 freshness
    }

    @Test
    void calculate_valuationWithDcfAndSensitivity_returnsCompleteScore() {
        LocalDate analysisDate = LocalDate.of(2026, 10, 15);
        ReverseDcfSensitivityMatrixDto matrix = new ReverseDcfSensitivityMatrixDto(List.of(0.1), List.of(0.03), List.of(mockCell()));
        ReverseDcfDto dcf = new ReverseDcfDto(100.0, 10.0, 0.1, 0.1, 10, 0.03, 12.0, 10.0, "5Y", false, "OK", matrix);

        when(factSheetData.getReverseDcfResult()).thenReturn(dcf);
        when(factSheetData.getCompanyFinancialRatiosList(StatementVariant.TTM)).thenReturn(List.of());
        when(factSheetData.getCompanyFinancialRatiosList(StatementVariant.ANNUAL)).thenReturn(List.of());

        AgentContext ctx = AgentContext.builder()
                .analysisDate(analysisDate)
                .factSheetData(factSheetData)
                .build();

        var result = calculator.calculate(AgentType.VALUATION, ctx);

        // completeness is 0.70 (0.4 DCF + 0.3 matrix), no ratio date defaults to 180d ago -> freshness decayed
        assertThat(result.confidenceScore().data()).isLessThan(0.70);
    }

    private CompanyFinancialRatiosDto mockRatio(LocalDate reportDate) {
        return new CompanyFinancialRatiosDto(
                "id", 1, "AAPL", "USD", null, StatementVariant.TTM,
                2026, "Q3", reportDate, reportDate, reportDate,
                1000f, 800f, 500f, 400f, 100f, 0.2f, 0.25f, 0.15f,
                0.2f, 5000f, 1000f, 0.4f, 0.25f, 0.16f, 1.5f,
                1.2f, 0.5f, 5.0f, 10f, 45f, 30f, 0.1f, 0.12f, 0.15f,
                0.08f, 0.1f, 8, 1.1f, 1, 0, 1,
                4, 4, 4, 8.5f
        );
    }

    private ReverseDcfMatrixCellDto mockCell() {
        return new ReverseDcfMatrixCellDto(
                10.0, 3.0, 12.0, ValuationZone.FAIR, true
        );
    }
}
