package com.oraculum.analyst.service;

import com.oraculum.analyst.config.AnalystProperties;
import com.oraculum.analyst.dto.CitationRegistry;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.company.api.CompanyFinancialDataApi;
import com.oraculum.company.api.CompanyInsiderTransactionApi;
import com.oraculum.company.api.CompanyNewsApi;
import com.oraculum.company.api.CompanySharePriceApi;
import com.oraculum.company.api.CompanyTickerDocumentApi;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.*;
import com.oraculum.economy.api.EconomyDataApi;
import com.oraculum.economy.api.dto.MacroSummaryDto;
import com.oraculum.harvester.api.HarvesterLiveApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompanyFactSheetDataServiceTest {

    @Mock
    private CompanyFinancialDataApi companyFinancialDataApi;
    @Mock
    private CompanySharePriceApi companySharePriceApi;
    @Mock
    private CompanyNewsApi companyNewsApi;
    @Mock
    private EconomyDataApi economyDataApi;
    @Mock
    private CompanyInsiderTransactionApi companyInsiderTransactionApi;
    @Mock
    private HarvesterLiveApi harvesterLiveApi;
    @Mock
    private CompanyTickerDocumentApi companyTickerDocumentApi;
    @Mock
    private tools.jackson.databind.json.JsonMapper jsonMapper;
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
        when(company.industryName()).thenReturn("Technology");

        when(jsonMapper.writeValueAsString(any())).thenReturn("[]");

        when(analystProperties.factSheet().getAnnualFactSheetHistoryDate()).thenReturn(LocalDate.now().minusYears(10));
        when(analystProperties.factSheet().getQuarterlyFactSheetHistoryDate()).thenReturn(LocalDate.now().minusYears(2));
        when(analystProperties.sharePrice().getSharePriceHistoryDate()).thenReturn(LocalDate.now().minusYears(1));
        when(analystProperties.sharePrice().getMonthlySharePriceHistoryDate()).thenReturn(LocalDate.now().minusYears(10));
        when(analystProperties.news().getNewsHistoryDate()).thenReturn(LocalDate.now().minusDays(30));
        when(analystProperties.news().articleLimit()).thenReturn(50);
        when(analystProperties.insider().getTransactionHistoryDate()).thenReturn(LocalDate.now().minusYears(5));
    }

    @Test
    void create_returnsPopulatedFactSheet() {
        IncomeStatementDto is = mock(IncomeStatementDto.class);
        when(is.variant()).thenReturn(StatementVariant.ANNUAL);
        when(is.reportDate()).thenReturn(LocalDate.now());
        when(companyFinancialDataApi.getIncomeStatementsByCompanyId(anyInt(), any(LocalDate.class)))
                .thenReturn(List.of(is));

        BalanceSheetDto bs = mock(BalanceSheetDto.class);
        when(bs.variant()).thenReturn(StatementVariant.ANNUAL);
        when(bs.reportDate()).thenReturn(LocalDate.now());
        when(companyFinancialDataApi.getBalanceSheetsByCompanyId(anyInt(), any(LocalDate.class)))
                .thenReturn(List.of(bs));

        CashFlowStatementDto cf = mock(CashFlowStatementDto.class);
        when(cf.variant()).thenReturn(StatementVariant.ANNUAL);
        when(cf.reportDate()).thenReturn(LocalDate.now());
        when(companyFinancialDataApi.getCashFlowStatementsByCompanyId(anyInt(), any(LocalDate.class)))
                .thenReturn(List.of(cf));

        CompanyFinancialRatiosDto ratio = mock(CompanyFinancialRatiosDto.class);
        when(ratio.variant()).thenReturn(StatementVariant.ANNUAL);
        when(ratio.reportDate()).thenReturn(LocalDate.now());
        when(companyFinancialDataApi.getCompanyFinancialRatiosByCompanyId(anyInt(), any(LocalDate.class)))
                .thenReturn(List.of(ratio));

        SharePriceSignalDto signal = mock(SharePriceSignalDto.class);
        when(companySharePriceApi.getDailySharePriceSignalsByCompanyId(anyInt(), any(LocalDate.class)))
                .thenReturn(List.of(signal));
        when(companySharePriceApi.getMonthlySharePriceSignalsByCompanyId(anyInt(), any(LocalDate.class)))
                .thenReturn(List.of(signal));

        NewsTickerDto news = mock(NewsTickerDto.class);
        when(news.timePublished()).thenReturn(OffsetDateTime.now());
        when(companyNewsApi.getNewsByTicker(anyString(), any(LocalDate.class)))
                .thenReturn(List.of(news));

        TickerNewsSentimentDto sentiment = mock(TickerNewsSentimentDto.class);
        when(companyNewsApi.getNewsSentimentByTicker(anyString())).thenReturn(Optional.of(sentiment));

        InsiderTransactionSummaryDto insiderSummary = mock(InsiderTransactionSummaryDto.class);
        when(companyInsiderTransactionApi.getInsiderTransactionSummaryByTicker(anyString())).thenReturn(Optional.of(insiderSummary));

        InsiderTransactionTickerDto transaction = mock(InsiderTransactionTickerDto.class);
        when(companyInsiderTransactionApi.getInsiderTransactionsByTicker(anyString(), any(LocalDate.class)))
                .thenReturn(List.of(transaction));

        MacroSummaryDto macroSummary = mock(MacroSummaryDto.class);
        when(economyDataApi.getMacroeconomicSummary()).thenReturn(List.of(macroSummary));

        when(harvesterLiveApi.fetchEarningsEstimates(anyString())).thenReturn(Optional.empty());
        when(companyTickerDocumentApi.getDocumentsByTicker(any())).thenReturn(List.of());

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
        assertThat(data.getNewsSentimentAggregate()).isNotNull();
        assertThat(data.getInsiderTransactionSummary()).isNotNull();
        assertThat(data.getRecentInsiderTransactions()).isNotNull();
        assertThat(data.getMacroeconomicSummary()).isNotNull();
    }
}
