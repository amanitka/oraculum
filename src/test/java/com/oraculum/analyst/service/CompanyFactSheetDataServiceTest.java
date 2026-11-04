package com.oraculum.analyst.service;

import com.oraculum.analyst.config.AnalystProperties;
import com.oraculum.analyst.dto.CitationRegistry;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.analyst.service.valuation.ValuationScenarioCalculator;
import com.oraculum.company.api.CompanyFinancialDataApi;
import com.oraculum.company.api.CompanyInsiderTransactionApi;
import com.oraculum.company.api.CompanyInstitutionalFlowApi;
import com.oraculum.company.api.CompanySharePriceApi;
import com.oraculum.company.api.CompanyTickerDocumentApi;
import com.oraculum.company.api.CompanyValuationApi;
import com.oraculum.company.api.domain.StatementTemplate;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.BalanceSheetDto;
import com.oraculum.company.api.dto.CashFlowStatementDto;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.company.api.dto.HistoricalValuationSummaryDto;
import com.oraculum.company.api.dto.IncomeStatementDto;
import com.oraculum.company.api.dto.IndustryFinancialRatiosDto;
import com.oraculum.company.api.dto.InsiderTransactionByPersonDto;
import com.oraculum.company.api.dto.InsiderTransactionSummaryDto;
import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.company.api.dto.SharePriceSignalDto;
import com.oraculum.economy.api.EconomyDataApi;
import com.oraculum.economy.api.domain.MacroIndicator;
import com.oraculum.economy.api.dto.MacroSummaryDto;
import com.oraculum.harvester.api.HarvesterLiveApi;
import com.oraculum.harvester.api.dto.CompanyNewsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyFactSheetDataServiceTest {

    @Mock
    private CompanyFinancialDataApi companyFinancialDataApi;
    @Mock
    private CompanySharePriceApi companySharePriceApi;
    @Mock
    private EconomyDataApi economyDataApi;
    @Mock
    private CompanyInsiderTransactionApi companyInsiderTransactionApi;
    @Mock
    private HarvesterLiveApi harvesterLiveApi;
    @Mock
    private CompanyTickerDocumentApi companyTickerDocumentApi;
    @Mock
    private CompanyValuationApi companyValuationApi;
    @Mock
    private CompanyInstitutionalFlowApi companyInstitutionalFlowApi;

    private AnalystProperties analystProperties;
    private CompanyFactSheetDataService service;
    private CompanyDto company;

    @BeforeEach
    void setUp() {
        company = new CompanyDto(
                1, "AAPL", "US", "Apple Inc", "TECH", "Consumer Electronics", "Technology",
                "US0378331005", "Desc", 100000L, "USD", "0000320193", "037833100", null
        );
        analystProperties = new AnalystProperties(
                null,
                null,
                new AnalystProperties.FactSheet(5, 2),
                new AnalystProperties.SharePrice(30, 5),
                new AnalystProperties.News(30, 50, 5),
                null,
                100000
        );
        service = new CompanyFactSheetDataService(
                companyFinancialDataApi,
                companySharePriceApi,
                companyInsiderTransactionApi,
                harvesterLiveApi,
                economyDataApi,
                companyTickerDocumentApi,
                new JsonMapper(),
                analystProperties,
                companyValuationApi,
                companyInstitutionalFlowApi,
                new ValuationScenarioCalculator()
        );
    }

    @Test
    void create_populatesAllFields_successfully() {
        LocalDate date = LocalDate.now().minusDays(10);
        IncomeStatementDto isDto = new IncomeStatementDto(
                "is-1", 1, "US", StatementTemplate.GENERAL, StatementVariant.ANNUAL,
                "USD", 2025, "FY", date, date, date, OffsetDateTime.now(), "{}"
        );
        BalanceSheetDto bsDto = new BalanceSheetDto(
                "bs-1", 1, "US", StatementTemplate.GENERAL, StatementVariant.ANNUAL,
                "USD", 2025, "FY", date, date, date, OffsetDateTime.now(), "{}"
        );
        CashFlowStatementDto cfDto = new CashFlowStatementDto(
                "cf-1", 1, "US", StatementTemplate.GENERAL, StatementVariant.ANNUAL,
                "USD", 2025, "FY", date, date, date, OffsetDateTime.now(), "{}"
        );
        when(companyFinancialDataApi.getIncomeStatementsByCompanyId(anyInt(), any(LocalDate.class)))
                .thenReturn(List.of(isDto));
        when(companyFinancialDataApi.getBalanceSheetsByCompanyId(anyInt(), any(LocalDate.class)))
                .thenReturn(List.of(bsDto));
        when(companyFinancialDataApi.getCashFlowStatementsByCompanyId(anyInt(), any(LocalDate.class)))
                .thenReturn(List.of(cfDto));

        CompanyFinancialRatiosDto ratios = new CompanyFinancialRatiosDto(
                "r-1", 1, "AAPL", "USD", null, StatementVariant.ANNUAL,
                2025, "FY", date, date, date,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, 0, 0, 0
        );
        when(companyFinancialDataApi.getCompanyFinancialRatiosByCompanyId(anyInt(), any(LocalDate.class)))
                .thenReturn(List.of(ratios));

        IndustryFinancialRatiosDto indRatios = new IndustryFinancialRatiosDto(
                "Consumer Electronics", StatementVariant.TTM, 10, 0.25f, 0.45f, 0.20f, 0.5f, 1.2f, 0.30f, 0.22f, 0.10f
        );
        when(companyFinancialDataApi.getIndustryFinancialRatiosByIndustryName(anyString()))
                .thenReturn(List.of(indRatios));

        SharePriceSignalDto signal = SharePriceSignalDto.builder()
                .tradeDate(date)
                .sharePrice(220.0f)
                .marketCapitalization(3.3e12f)
                .build();
        when(companySharePriceApi.getDailySharePriceSignalsByCompanyId(anyInt(), any(LocalDate.class)))
                .thenReturn(List.of(signal));
        when(companySharePriceApi.getMonthlySharePriceSignalsByCompanyId(anyInt(), any(LocalDate.class)))
                .thenReturn(List.of(signal));

        CompanyNewsDto news = CompanyNewsDto.builder()
                .id(1L)
                .publishedAt(OffsetDateTime.now())
                .headline("Apple launches new product")
                .build();
        when(harvesterLiveApi.fetchCompanyNews(eq("AAPL"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(news));

        InsiderTransactionSummaryDto insiderSummary = InsiderTransactionSummaryDto.builder()
                .ticker("AAPL")
                .hasClusterBuy(false)
                .build();
        when(companyInsiderTransactionApi.getInsiderTransactionSummaryByTicker(anyString()))
                .thenReturn(Optional.of(insiderSummary));

        InsiderTransactionByPersonDto personTx = InsiderTransactionByPersonDto.builder()
                .ticker("AAPL")
                .insiderName("Tim Cook")
                .build();
        when(companyInsiderTransactionApi.getInsiderTransactionsByPerson(anyString()))
                .thenReturn(List.of(personTx));

        MacroSummaryDto macroSummary = new MacroSummaryDto(
                MacroIndicator.GS10, "10Y Treasury", date, 4.2, 4.1, 2.4, 10.0, 3.8, 4.7, 4.2, 0.0
        );
        when(economyDataApi.getMacroeconomicSummary()).thenReturn(List.of(macroSummary));

        when(harvesterLiveApi.fetchEarningsEstimates(anyString())).thenReturn(Optional.empty());
        when(companyTickerDocumentApi.getDocumentsForAnalysisByTicker(any())).thenReturn(List.of());

        ReverseDcfDto reverseDcf = new ReverseDcfDto(
                3.3e12, 1.0e11, 0.03, 0.10, 10, 0.03, 9.5, 11.0, "5Y", false, "Fair", null
        );
        when(companyValuationApi.calculateReverseDcf(anyInt())).thenReturn(reverseDcf);

        HistoricalValuationSummaryDto histSummary = new HistoricalValuationSummaryDto(
                "P/E", 30.0f, 28.0f, 25.0f, 65, 15.0f, 38.0f
        );
        when(companyValuationApi.calculateHistoricalValuationPercentiles(anyInt())).thenReturn(List.of(histSummary));

        CompanyFactSheetData data = service.create(company, new CitationRegistry());

        assertThat(data).isNotNull();
        assertThat(data.getCompany()).isEqualTo(company);
        assertThat(data.getIncomeStatements()).containsKeys(StatementVariant.ANNUAL);
        assertThat(data.getBalanceSheets()).containsKeys(StatementVariant.ANNUAL);
        assertThat(data.getCashFlowStatements()).containsKeys(StatementVariant.ANNUAL);
        assertThat(data.getCompanyFinancialRatios()).containsKeys(StatementVariant.ANNUAL);
        assertThat(data.getDailySharePriceSignals()).isNotNull();
        assertThat(data.getMonthlySharePriceSignals()).isNotNull();
        assertThat(data.getRecentNewsList()).containsExactly(news);
        assertThat(data.getInsiderTransactionSummary()).isNotNull();
        assertThat(data.getInsiderTransactionsByPerson()).isNotNull();
        assertThat(data.getMacroeconomicSummary()).isNotNull();
        assertThat(data.getInstitutionalFlowHistory()).isNotNull();
    }

    @Test
    void create_delegatesUnfilteredNewsFetchToHarvesterLiveApi() {
        LocalDate historyDate = analystProperties.news().getNewsHistoryDate();
        CompanyNewsDto newsItem = CompanyNewsDto.builder()
                .id(2L)
                .publishedAt(OffsetDateTime.now())
                .headline("AAPL update")
                .build();
        when(harvesterLiveApi.fetchCompanyNews(eq("AAPL"), eq(historyDate), any(LocalDate.class)))
                .thenReturn(List.of(newsItem));

        CompanyFactSheetData data = service.create(company, new CitationRegistry());

        verify(harvesterLiveApi).fetchCompanyNews(eq("AAPL"), eq(historyDate), any(LocalDate.class));
        assertThat(data.getRecentNewsList()).containsExactly(newsItem);
    }
}
