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

    private Map<StatementVariant, List<CompanyFinancialRatiosDto>> getCompanyFinancialRatios(CompanyDto company, LocalDate after) {
        return companyApi.getCompanyFinancialRatiosByCompanyId(company.id(), after)
                .stream()
                .collect(Collectors.groupingBy(CompanyFinancialRatiosDto::variant));
    }

    private List<SharePriceSignalDto> getDailySharePriceSignals(CompanyDto company, LocalDate after) {
        return companyApi.getDailySharePriceSignalsByCompanyId(company.id(), after);
    }

    private List<SharePriceSignalDto> getMonthlySharePriceSignals(CompanyDto company, LocalDate after) {
        return companyApi.getMonthlySharePriceSignalsByCompanyId(company.id(), after);
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
                .companyFinancialRatios(getCompanyFinancialRatios(company, factSheetAfter))
                .dailySharePriceSignals(getDailySharePriceSignals(company,
                        analystProperties.sharePrice().getSharePriceHistoryDate()))
                .monthlySharePriceSignals(getMonthlySharePriceSignals(company,
                        analystProperties.sharePrice().getMonthlySharePriceHistoryDate()))
                .recentNews(getNews(company, analystProperties.news().getNewsHistoryDate()))
                .build();
    }
}