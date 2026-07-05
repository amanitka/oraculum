package com.oraculum.analyst.service;

import com.oraculum.analyst.config.AnalystProperties;
import com.oraculum.analyst.dto.CitationRegistry;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.company.api.CompanyFinancialDataApi;
import com.oraculum.company.api.CompanyInsiderTransactionApi;
import com.oraculum.company.api.CompanyNewsApi;
import com.oraculum.company.api.CompanySharePriceApi;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.*;
import com.oraculum.economy.api.EconomyDataApi;
import com.oraculum.harvester.api.HarvesterLiveApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyFactSheetDataService {

    private final CompanyFinancialDataApi companyFinancialDataApi;
    private final CompanySharePriceApi companySharePriceApi;
    private final CompanyNewsApi companyNewsApi;
    private final CompanyInsiderTransactionApi companyInsiderTransactionApi;
    private final HarvesterLiveApi harvesterLiveApi;
    private final EconomyDataApi economyDataApi;
    private final JsonMapper jsonMapper;
    private final AnalystProperties analystProperties;

    private Map<StatementVariant, List<IncomeStatementDto>> getIncomeStatements(CompanyDto company, LocalDate annualAfter, LocalDate quarterlyAfter) {
        return companyFinancialDataApi.getIncomeStatementsByCompanyId(company.id(), annualAfter)
                .stream()
                .filter(dto -> dto.variant() == StatementVariant.ANNUAL || !dto.reportDate().isBefore(quarterlyAfter))
                .collect(Collectors.groupingBy(IncomeStatementDto::variant));
    }

    private Map<StatementVariant, List<BalanceSheetDto>> getBalanceSheets(CompanyDto company, LocalDate annualAfter, LocalDate quarterlyAfter) {
        return companyFinancialDataApi.getBalanceSheetsByCompanyId(company.id(), annualAfter)
                .stream()
                .filter(dto -> dto.variant() == StatementVariant.ANNUAL || !dto.reportDate().isBefore(quarterlyAfter))
                .collect(Collectors.groupingBy(BalanceSheetDto::variant));
    }

    private Map<StatementVariant, List<CashFlowStatementDto>> getCashFlowStatements(CompanyDto company,
                                                                                    LocalDate annualAfter, LocalDate quarterlyAfter) {
        return companyFinancialDataApi.getCashFlowStatementsByCompanyId(company.id(), annualAfter)
                .stream()
                .filter(dto -> dto.variant() == StatementVariant.ANNUAL || !dto.reportDate().isBefore(quarterlyAfter))
                .collect(Collectors.groupingBy(CashFlowStatementDto::variant));
    }

    private Map<StatementVariant, List<CompanyFinancialRatiosDto>> getCompanyFinancialRatios(CompanyDto company,
                                                                                             LocalDate annualAfter, LocalDate quarterlyAfter) {
        return companyFinancialDataApi.getCompanyFinancialRatiosByCompanyId(company.id(), annualAfter)
                .stream()
                .filter(dto -> dto.variant() == StatementVariant.ANNUAL || !dto.reportDate().isBefore(quarterlyAfter))
                .collect(Collectors.groupingBy(CompanyFinancialRatiosDto::variant));
    }

    private Map<StatementVariant, List<IndustryFinancialRatiosDto>> getIndustryFinancialRatios(CompanyDto company) {
        if (company.industryName() == null || company.industryName().isBlank()) {
            return Map.of();
        }
        return companyFinancialDataApi.getIndustryFinancialRatiosByIndustryName(company.industryName())
                .stream()
                .collect(Collectors.groupingBy(IndustryFinancialRatiosDto::variant));
    }

    private List<SharePriceSignalDto> getDailySharePriceSignals(CompanyDto company, LocalDate after) {
        return companySharePriceApi.getDailySharePriceSignalsByCompanyId(company.id(), after);
    }

    private List<SharePriceSignalDto> getMonthlySharePriceSignals(CompanyDto company, LocalDate after) {
        return companySharePriceApi.getMonthlySharePriceSignalsByCompanyId(company.id(), after);
    }

    private List<NewsTickerDto> getNews(CompanyDto company, LocalDate after, int limit) {
        List<NewsTickerDto> news = companyNewsApi.getNewsByTicker(company.ticker(), after);
        if (news == null || news.isEmpty()) return news;

        return news.stream()
                .sorted(Comparator.comparing(NewsTickerDto::relevanceScore, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                        .thenComparing(NewsTickerDto::timePublished, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .limit(limit > 0 ? limit : 50)
                .sorted(Comparator.comparing(NewsTickerDto::timePublished, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private TickerNewsSentimentDto getNewsSentiment(CompanyDto company) {
        return companyNewsApi.getNewsSentimentByTicker(company.ticker()).orElse(null);
    }

    private InsiderTransactionSummaryDto getInsiderTransactionSummary(CompanyDto company) {
        return companyInsiderTransactionApi.getInsiderTransactionSummaryByTicker(company.ticker()).orElse(null);
    }

    private List<InsiderTransactionTickerDto> getRecentInsiderTransactions(CompanyDto company, LocalDate after) {
        return companyInsiderTransactionApi.getInsiderTransactionsByTicker(company.ticker(), after);
    }

    public CompanyFactSheetData create(CompanyDto company, CitationRegistry citationRegistry) {
        LocalDate annualAfter = analystProperties.factSheet().getAnnualFactSheetHistoryDate();
        LocalDate quarterlyAfter = analystProperties.factSheet().getQuarterlyFactSheetHistoryDate();

        return CompanyFactSheetData.builder()
                .jsonMapper(jsonMapper)
                .company(company)
                .citationRegistry(citationRegistry)
                .incomeStatements(getIncomeStatements(company, annualAfter, quarterlyAfter))
                .balanceSheets(getBalanceSheets(company, annualAfter, quarterlyAfter))
                .cashFlowStatements(getCashFlowStatements(company, annualAfter, quarterlyAfter))
                .companyFinancialRatios(getCompanyFinancialRatios(company, annualAfter, quarterlyAfter))
                .industryFinancialRatios(getIndustryFinancialRatios(company))
                .dailySharePriceSignals(getDailySharePriceSignals(company,
                        analystProperties.sharePrice().getSharePriceHistoryDate()))
                .monthlySharePriceSignals(getMonthlySharePriceSignals(company,
                        analystProperties.sharePrice().getMonthlySharePriceHistoryDate()))
                .recentNews(getNews(company, analystProperties.news().getNewsHistoryDate(), analystProperties.news().articleLimit()))
                .newsSentimentAggregate(getNewsSentiment(company))
                .insiderTransactionSummary(getInsiderTransactionSummary(company))
                .recentInsiderTransactions(getRecentInsiderTransactions(company, analystProperties.insider().getTransactionHistoryDate()))
                .earningsEstimates(harvesterLiveApi.fetchEarningsEstimates(company.ticker()).orElse(List.of()))
                .macroeconomicSummary(economyDataApi.getMacroeconomicSummary())
                .build();
    }
}
