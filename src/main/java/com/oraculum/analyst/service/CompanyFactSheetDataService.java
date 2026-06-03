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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyFactSheetDataService {

    private final CompanyApi companyApi;
    private final ObjectMapper objectMapper;
    private final AnalystProperties analystProperties;

    private Map<StatementVariant, List<IncomeStatementDto>> getIncomeStatements(CompanyDto company, LocalDate after) {
        return companyApi.getIncomeStatementsByCompanyId(company.id(), after)
                .stream()
                .collect(Collectors.groupingBy(IncomeStatementDto::variant));
    }

    private Map<StatementVariant, List<BalanceSheetDto>> getBalanceSheets(CompanyDto company, LocalDate after) {
        return companyApi.getBalanceSheetsByCompanyId(company.id(), after)
                .stream()
                .collect(Collectors.groupingBy(BalanceSheetDto::variant));
    }

    private Map<StatementVariant, List<CashFlowStatementDto>> getCashFlowStatements(CompanyDto company,
                                                                                    LocalDate after) {
        return companyApi.getCashFlowStatementsByCompanyId(company.id(), after)
                .stream()
                .collect(Collectors.groupingBy(CashFlowStatementDto::variant));
    }

    private Map<StatementVariant, List<DerivedMetricsDto>> getDerivedMetrics(CompanyDto company, LocalDate after) {
        return companyApi.getDerivedMetricsByCompanyId(company.id(), after)
                .stream()
                .collect(Collectors.groupingBy(DerivedMetricsDto::variant));
    }

    private List<DailyMarketSignalDto> getDailyMarketSignals(CompanyDto company, LocalDate after) {
        return companyApi.getDailyMarketSignalsByCompanyId(company.id(), after);
    }

    private List<DailyMarketSignalDto> getMonthlyMarketSignals(CompanyDto company, LocalDate after) {
        return companyApi.getMonthlyMarketSignalsByCompanyId(company.id(), after);
    }

    private List<NewsTickerDto> getNews(CompanyDto company, LocalDate after) {
        return companyApi.getNewsByTicker(company.ticker(), after);
    }

    public CompanyFactSheetData create(CompanyDto company) {
        LocalDate factSheetAfter = analystProperties.factSheet().getFactSheetHistoryDate();

        return CompanyFactSheetData.builder()
                .objectMapper(objectMapper)
                .company(company)
                .incomeStatements(getIncomeStatements(company, factSheetAfter))
                .balanceSheets(getBalanceSheets(company, factSheetAfter))
                .cashFlowStatements(getCashFlowStatements(company, factSheetAfter))
                .derivedMetrics(getDerivedMetrics(company, factSheetAfter))
                .dailyMarketSignals(getDailyMarketSignals(company,
                        analystProperties.sharePrice().getSharePriceHistoryDate()))
                .monthlyMarketSignals(getMonthlyMarketSignals(company,
                        analystProperties.sharePrice().getMonthlySharePriceHistoryDate()))
                .recentNews(getNews(company, analystProperties.news().getNewsHistoryDate()))
                .build();
    }
}
