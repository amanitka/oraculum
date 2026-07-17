package com.oraculum.analyst.dto;

import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.company.api.dto.SharePriceSignalDto;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompanyFactSheetDataTest {

    private JsonMapper jsonMapper;
    private CitationRegistry citationRegistry;
    private CompanyDto company;

    @BeforeEach
    void setUp() {
        jsonMapper = new JsonMapper();
        citationRegistry = new CitationRegistry();
        company = mock(CompanyDto.class);
        when(company.id()).thenReturn(1);
    }

    @Test
    void getReverseDcfAnalysis_returnsSerializedResult() {
        ReverseDcfResult reverseDcfResult = new ReverseDcfResult(
                10_000_000_000f, 500_000_000f, 5.0f, 10.0f, 10, 3.0f, 12.5f, 15.0f, "Test Interpretation"
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
        HistoricalValuationSummary summary = new HistoricalValuationSummary(
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
}
