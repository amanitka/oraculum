package com.oraculum.analyst.service;

import com.oraculum.analyst.config.AnalystProperties;
import com.oraculum.analyst.dto.CitationRegistry;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.company.api.CompanyFinancialDataApi;
import com.oraculum.company.api.CompanyInsiderTransactionApi;
import com.oraculum.company.api.CompanyInstitutionalFlowApi;
import com.oraculum.company.api.CompanySharePriceApi;
import com.oraculum.company.api.CompanyTickerDocumentApi;
import com.oraculum.company.api.CompanyValuationApi;
import com.oraculum.company.api.domain.StatementVariant;
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
import com.oraculum.economy.api.dto.MacroSummaryDto;
import com.oraculum.harvester.api.HarvesterLiveApi;
import com.oraculum.harvester.api.dto.CompanyNewsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
    @Mock
    private JsonMapper jsonMapper;
    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private AnalystProperties analystProperties;

    @InjectMocks
    private CompanyFactSheetDataService service;

    private CompanyDto company;

    @BeforeEach
    void setUp() {
        company = mock(CompanyDto.class);
        when(company.id()).thenReturn(1);
        when(company.ticker()).thenReturn("AAPL");
        when(company.industryId()).thenReturn("TECH");
        when(company.market()).thenReturn("US");

        when(analystProperties.factSheet().getAnnualFactSheetHistoryDate()).thenReturn(LocalDate.now().minusYears(5));
        when(analystProperties.factSheet().getQuarterlyFactSheetHistoryDate()).thenReturn(LocalDate.now().minusYears(2));
        when(analystProperties.sharePrice().getSharePriceHistoryDate()).thenReturn(LocalDate.now().minusDays(30));
        when(analystProperties.sharePrice().getMonthlySharePriceHistoryDate()).thenReturn(LocalDate.now().minusYears(5));
        when(analystProperties.news().getNewsHistoryDate()).thenReturn(LocalDate.now().minusDays(30));
        when(analystProperties.news().articleLimit()).thenReturn(50);
    }

    @Test
    void create_populatesAllFields_successfully() {
        IncomeStatementDto isDto = mock(IncomeStatementDto.class);
        when(companyFinancialDataApi.getIncomeStatementsByCompanyId(anyInt(), any(LocalDate.class)))
                .thenReturn(List.of(isDto));

        CompanyFinancialRatiosDto ratios = mock(CompanyFinancialRatiosDto.class);
        when(companyFinancialDataApi.getCompanyFinancialRatiosByCompanyId(anyInt(), any(LocalDate.class)))
                .thenReturn(List.of(ratios));

        IndustryFinancialRatiosDto indRatios = mock(IndustryFinancialRatiosDto.class);
        when(companyFinancialDataApi.getIndustryFinancialRatiosByIndustryName(anyString()))
                .thenReturn(List.of(indRatios));

        SharePriceSignalDto signal = mock(SharePriceSignalDto.class);
        when(companySharePriceApi.getDailySharePriceSignalsByCompanyId(anyInt(), any(LocalDate.class)))
                .thenReturn(List.of(signal));
        when(companySharePriceApi.getMonthlySharePriceSignalsByCompanyId(anyInt(), any(LocalDate.class)))
                .thenReturn(List.of(signal));

        CompanyNewsDto news = mock(CompanyNewsDto.class);
        when(harvesterLiveApi.fetchCompanyNews(eq("AAPL"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(news));

        InsiderTransactionSummaryDto insiderSummary = mock(InsiderTransactionSummaryDto.class);
        when(companyInsiderTransactionApi.getInsiderTransactionSummaryByTicker(anyString())).thenReturn(Optional.of(insiderSummary));

        InsiderTransactionByPersonDto personTx = mock(InsiderTransactionByPersonDto.class);
        when(companyInsiderTransactionApi.getInsiderTransactionsByPerson(anyString()))
                .thenReturn(List.of(personTx));

        MacroSummaryDto macroSummary = mock(MacroSummaryDto.class);
        when(economyDataApi.getMacroeconomicSummary()).thenReturn(List.of(macroSummary));

        when(harvesterLiveApi.fetchEarningsEstimates(anyString())).thenReturn(Optional.empty());
        when(companyTickerDocumentApi.getDocumentsForAnalysisByTicker(any())).thenReturn(List.of());

        ReverseDcfDto reverseDcf = mock(ReverseDcfDto.class);
        when(companyValuationApi.calculateReverseDcf(anyInt())).thenReturn(reverseDcf);

        HistoricalValuationSummaryDto histSummary = mock(HistoricalValuationSummaryDto.class);
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
        assertThat(data.getRecentNews()).isNotNull();
        assertThat(data.getRecentNewsList()).containsExactly(news);
        assertThat(data.getInsiderTransactionSummary()).isNotNull();
        assertThat(data.getInsiderTransactionsByPerson()).isNotNull();
        assertThat(data.getMacroeconomicSummary()).isNotNull();
        assertThat(data.getInstitutionalFlowHistory()).isNotNull();
    }

    @Test
    void create_delegatesUnfilteredNewsFetchToHarvesterLiveApi() {
        LocalDate historyDate = LocalDate.now().minusDays(30);
        when(analystProperties.news().getNewsHistoryDate()).thenReturn(historyDate);

        CompanyNewsDto newsItem = mock(CompanyNewsDto.class);
        when(harvesterLiveApi.fetchCompanyNews(eq("AAPL"), eq(historyDate), any(LocalDate.class)))
                .thenReturn(List.of(newsItem));

        CompanyFactSheetData data = service.create(company, new CitationRegistry());

        verify(harvesterLiveApi).fetchCompanyNews(eq("AAPL"), eq(historyDate), any(LocalDate.class));
        assertThat(data.getRecentNewsList()).containsExactly(newsItem);
    }
}
