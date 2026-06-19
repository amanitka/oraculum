package com.oraculum.analyst.service;

import com.oraculum.analyst.config.AnalystProperties;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.company.api.CompanyApi;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyFactSheetDataService {

    private final CompanyApi companyApi;
    private final ObjectMapper objectMapper;
    private final AnalystProperties analystProperties;

    private Map<StatementVariant, List<IncomeStatementDto>> getIncomeStatements(CompanyDto company, LocalDate annualAfter, LocalDate quarterlyAfter) {
        return companyApi.getIncomeStatementsByCompanyId(company.id(), annualAfter)
                .stream()
                .filter(dto -> dto.variant() == StatementVariant.ANNUAL || !dto.reportDate().isBefore(quarterlyAfter))
                .collect(Collectors.groupingBy(IncomeStatementDto::variant));
    }

    private Map<StatementVariant, List<BalanceSheetDto>> getBalanceSheets(CompanyDto company, LocalDate annualAfter, LocalDate quarterlyAfter) {
        return companyApi.getBalanceSheetsByCompanyId(company.id(), annualAfter)
                .stream()
                .filter(dto -> dto.variant() == StatementVariant.ANNUAL || !dto.reportDate().isBefore(quarterlyAfter))
                .collect(Collectors.groupingBy(BalanceSheetDto::variant));
    }

    private Map<StatementVariant, List<CashFlowStatementDto>> getCashFlowStatements(CompanyDto company,
                                                                                    LocalDate annualAfter, LocalDate quarterlyAfter) {
        return companyApi.getCashFlowStatementsByCompanyId(company.id(), annualAfter)
                .stream()
                .filter(dto -> dto.variant() == StatementVariant.ANNUAL || !dto.reportDate().isBefore(quarterlyAfter))
                .collect(Collectors.groupingBy(CashFlowStatementDto::variant));
    }

    private Map<StatementVariant, List<CompanyFinancialRatiosDto>> getCompanyFinancialRatios(CompanyDto company,
                                                                                             LocalDate annualAfter, LocalDate quarterlyAfter) {
        return companyApi.getCompanyFinancialRatiosByCompanyId(company.id(), annualAfter)
                .stream()
                .filter(dto -> dto.variant() == StatementVariant.ANNUAL || !dto.reportDate().isBefore(quarterlyAfter))
                .collect(Collectors.groupingBy(CompanyFinancialRatiosDto::variant));
    }

    private Map<StatementVariant, List<IndustryFinancialRatiosDto>> getIndustryFinancialRatios(CompanyDto company,
                                                                                               LocalDate annualAfter, LocalDate quarterlyAfter) {
        if (company.industryName() == null || company.industryName().isBlank()) {
            return Map.of();
        }
        return companyApi.getIndustryFinancialRatiosByIndustryName(company.industryName(), annualAfter)
                .stream()
                // Assuming industry ratios lack report dates, we fetch all. Filtering might need to happen by year if needed, but grouping is fine.
                .collect(Collectors.groupingBy(IndustryFinancialRatiosDto::variant));
    }

    private List<SharePriceSignalDto> getDailySharePriceSignals(CompanyDto company, LocalDate after) {
        return companyApi.getDailySharePriceSignalsByCompanyId(company.id(), after);
    }

    private List<SharePriceSignalDto> getMonthlySharePriceSignals(CompanyDto company, LocalDate after) {
        return companyApi.getMonthlySharePriceSignalsByCompanyId(company.id(), after);
    }

    private List<NewsTickerDto> getNews(CompanyDto company, LocalDate after, int limit) {
        List<NewsTickerDto> news = companyApi.getNewsByTicker(company.ticker(), after);
        if (news == null || news.isEmpty()) return news;

        return news.stream()
                .sorted(Comparator.comparing(NewsTickerDto::relevanceScore, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                        .thenComparing(NewsTickerDto::timePublished, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .limit(limit > 0 ? limit : 50)
                .sorted(Comparator.comparing(NewsTickerDto::timePublished, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private TickerNewsSentimentDto getNewsSentiment(CompanyDto company) {
        return companyApi.getNewsSentimentByTicker(company.ticker()).orElse(null);
    }

    public CompanyFactSheetData create(CompanyDto company) {
        LocalDate annualAfter = analystProperties.factSheet().getAnnualFactSheetHistoryDate();
        LocalDate quarterlyAfter = analystProperties.factSheet().getQuarterlyFactSheetHistoryDate();

        return CompanyFactSheetData.builder()
                .objectMapper(objectMapper)
                .company(company)
                .incomeStatements(getIncomeStatements(company, annualAfter, quarterlyAfter))
                .balanceSheets(getBalanceSheets(company, annualAfter, quarterlyAfter))
                .cashFlowStatements(getCashFlowStatements(company, annualAfter, quarterlyAfter))
                .companyFinancialRatios(getCompanyFinancialRatios(company, annualAfter, quarterlyAfter))
                .industryFinancialRatios(getIndustryFinancialRatios(company, annualAfter, quarterlyAfter))
                .dailySharePriceSignals(getDailySharePriceSignals(company,
                        analystProperties.sharePrice().getSharePriceHistoryDate()))
                .monthlySharePriceSignals(getMonthlySharePriceSignals(company,
                        analystProperties.sharePrice().getMonthlySharePriceHistoryDate()))
                .recentNews(getNews(company, analystProperties.news().getNewsHistoryDate(), analystProperties.news().articleLimit()))
                .newsSentimentAggregate(getNewsSentiment(company))
                .build();
    }
}