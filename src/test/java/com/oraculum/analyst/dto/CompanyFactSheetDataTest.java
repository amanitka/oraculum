package com.oraculum.analyst.dto;

import com.oraculum.company.api.domain.StatementTemplate;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.domain.TickerDocumentSubtype;
import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.company.api.dto.HistoricalValuationSummaryDto;
import com.oraculum.company.api.dto.IncomeStatementDto;
import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.company.api.dto.ReverseDcfSensitivityMatrixDto;
import com.oraculum.company.api.dto.TickerDocumentDto;
import com.oraculum.harvester.api.dto.CompanyNewsDto;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyFactSheetDataTest {

    private JsonMapper jsonMapper;
    private CitationRegistry citationRegistry;
    private CompanyDto company;

    @BeforeEach
    void setUp() {
        jsonMapper = new JsonMapper();
        citationRegistry = new CitationRegistry();
        company = new CompanyDto(1, "NVDA", "us", "NVIDIA Corp", null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void getReverseDcfAnalysis_returnsSerializedResult() {
        ReverseDcfSensitivityMatrixDto matrix = new ReverseDcfSensitivityMatrixDto(List.of(10.0), List.of(3.0), List.of());
        ReverseDcfDto reverseDcfResult = new ReverseDcfDto(
                10_000_000_000d, 500_000_000d, 5.0d, 10.0d, 10, 3.0d, 12.5d, 15.0d,
                "FY2020 to FY2025 (5-year period)", false, "Test Interpretation", matrix
        );

        CompanyFactSheetData data = CompanyFactSheetData.builder()
                .jsonMapper(jsonMapper)
                .citationRegistry(citationRegistry)
                .company(company)
                .reverseDcfResult(reverseDcfResult)
                .build();

        String json = data.getReverseDcfAnalysis();
        assertThat(json).contains("\"current_market_cap\":1.0E10");
        assertThat(json).contains("\"implied_fcf_growth_rate_pct\":12.5");
        assertThat(json).contains("Test Interpretation");
    }

    @Test
    void getHistoricalValuationPercentiles_returnsSerializedList() {
        HistoricalValuationSummaryDto summary = new HistoricalValuationSummaryDto(
                "P/E", 20.0f, 18.0f, 15.0f, 60, 10.0f, 25.0f
        );

        CompanyFactSheetData data = CompanyFactSheetData.builder()
                .jsonMapper(jsonMapper)
                .citationRegistry(citationRegistry)
                .company(company)
                .historicalValuationPercentiles(List.of(summary))
                .build();

        String json = data.getHistoricalValuationPercentiles();
        assertThat(json).contains("\"metric\":\"P/E\"");
        assertThat(json).contains("\"current\":20.0");
        assertThat(json).contains("\"avg_5y\":18.0");
        assertThat(json).contains("\"avg_10y\":15.0");
        assertThat(json).contains("\"percentile_10y\":60");
        assertThat(json).contains("\"min_10y\":10.0");
        assertThat(json).contains("\"max_10y\":25.0");
    }

    @Test
    void getFutureEarningsEstimates_assignsDistinctCitationIdsForSameDateDifferentHorizons() {
        EarningsEstimateDto fy27 = new EarningsEstimateDto(
                LocalDate.of(2027, 1, 31), "fiscal year", BigDecimal.valueOf(9.31),
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        EarningsEstimateDto q4Fy27 = new EarningsEstimateDto(
                LocalDate.of(2027, 1, 31), "fiscal quarter", BigDecimal.valueOf(2.75),
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );

        CompanyFactSheetData data = CompanyFactSheetData.builder()
                .jsonMapper(jsonMapper)
                .citationRegistry(citationRegistry)
                .company(company)
                .earningsEstimates(List.of(fy27, q4Fy27))
                .build();

        String json = data.getFutureEarningsEstimates(LocalDate.of(2026, 10, 25));
        assertThat(json).contains("\"citation_id\":\"1\"");
        assertThat(json).contains("\"citation_id\":\"2\"");
        assertThat(citationRegistry.getCitations()).containsKeys(1, 2);
    }

    @Test
    void getRecentCompanyNews_and_getRecentMarketNews_separateDirectFromContextArticles() {
        CompanyNewsDto direct = new CompanyNewsDto(
                10L, OffsetDateTime.now(), "Nvidia launches next-gen AI rack", "Strong demand for NVDA chips",
                "Reuters", "https://url1", List.of("NVDA")
        );
        CompanyNewsDto marketNoise = new CompanyNewsDto(
                20L, OffsetDateTime.now(), "3 Insurance Stocks from our Screener", "Hannover Re looks cheap",
                "Yahoo", "https://url2", List.of("NVDA")
        );

        CompanyFactSheetData data = CompanyFactSheetData.builder()
                .jsonMapper(jsonMapper)
                .citationRegistry(citationRegistry)
                .company(company)
                .recentNews(List.of(marketNoise, direct))
                .build();

        String companyJson = data.getRecentCompanyNews();
        assertThat(companyJson).contains("Nvidia launches next-gen AI rack");
        assertThat(companyJson).doesNotContain("3 Insurance Stocks from our Screener");

        String marketJson = data.getRecentMarketNews();
        assertThat(marketJson).contains("3 Insurance Stocks from our Screener");
        assertThat(marketJson).doesNotContain("Nvidia launches next-gen AI rack");

        assertThat(data.getRecentCompanyNewsList()).containsExactly(direct);
        assertThat(data.getRecentMarketNewsList()).containsExactly(marketNoise);
    }

    @Test
    void getRecentSecEx991Summaries_withAgeLimit_filtersOutOlder8KFilings() {
        TickerDocumentDto recent8k = TickerDocumentDto.builder()
                .id("doc-recent")
                .documentType(TickerDocumentType.SEC_8K)
                .documentSubtype(TickerDocumentSubtype.SEC_EX99_1)
                .reportPeriod(LocalDate.of(2026, 8, 19))
                .filingDate(LocalDate.of(2026, 8, 26))
                .summary("{\"headline\":\"Q2 Earnings\"}")
                .build();
        TickerDocumentDto oldMay8k = TickerDocumentDto.builder()
                .id("doc-old")
                .documentType(TickerDocumentType.SEC_8K)
                .documentSubtype(TickerDocumentSubtype.SEC_EX99_1)
                .reportPeriod(LocalDate.of(2026, 5, 28))
                .filingDate(LocalDate.of(2026, 5, 28))
                .summary("{\"headline\":\"Q1 Earnings\"}")
                .build();

        CompanyFactSheetData data = CompanyFactSheetData.builder()
                .jsonMapper(jsonMapper)
                .citationRegistry(citationRegistry)
                .company(company)
                .recentSecDocuments(Map.of(
                        TickerDocumentType.SEC_8K,
                        Map.of(TickerDocumentSubtype.SEC_EX99_1, List.of(recent8k, oldMay8k))
                ))
                .build();

        String filtered = data.getRecentSecEx991Summaries(LocalDate.of(2026, 10, 25), 75);
        assertThat(filtered).contains("Q2 Earnings");
        assertThat(filtered).doesNotContain("Q1 Earnings");
    }

    @Test
    void getIncomeStatementHistory_injectsPeriodEndLabel() {
        IncomeStatementDto stmt = new IncomeStatementDto(
                "stmt-1", 1, "us", StatementTemplate.GENERAL, StatementVariant.QUARTERLY, "USD",
                2026, "Q2", LocalDate.of(2026, 7, 31), LocalDate.of(2026, 8, 26),
                LocalDate.of(2026, 8, 26), OffsetDateTime.now(),
                "{\"Report Date\":\"2026-07-31\",\"Fiscal Period\":\"Q2\",\"Fiscal Year\":2026,\"Revenue\":68127}"
        );

        CompanyFactSheetData data = CompanyFactSheetData.builder()
                .jsonMapper(jsonMapper)
                .citationRegistry(citationRegistry)
                .company(company)
                .incomeStatements(Map.of(StatementVariant.QUARTERLY, List.of(stmt)))
                .build();

        String json = data.getIncomeStatementHistory(StatementVariant.QUARTERLY, 8);
        assertThat(json).contains("\"Period End Label\":\"Q2 (ended 2026-07-31)\"");
    }
}
