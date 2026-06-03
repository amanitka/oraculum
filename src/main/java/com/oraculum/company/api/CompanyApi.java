package com.oraculum.company.api;

import com.oraculum.company.api.dto.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Public API for the Company module.
 */
public interface CompanyApi {

    CompanyDto getCompany(String ticker, String market);

    CompanyDto getCompanyById(int companyId);

    List<CompanyDto> getAllCompanies();

    List<MarketDto> getAllMarkets();

    List<IndustryDto> getAllIndustries();

    List<NewsTickerDto> getNewsByTicker(String ticker, LocalDate after);

    List<BalanceSheetDto> getBalanceSheetsByCompanyId(int companyId, LocalDate after);

    List<CashFlowStatementDto> getCashFlowStatementsByCompanyId(int companyId, LocalDate after);

    List<IncomeStatementDto> getIncomeStatementsByCompanyId(int companyId, LocalDate after);

    List<SharePriceDto> getSharePricesByCompanyId(int companyId, LocalDate after);

    List<SharePriceDto> getMonthlySharePricesByCompanyId(int companyId, LocalDate after);

    List<DailyMarketSignalDto> getDailyMarketSignalsByCompanyId(int companyId, LocalDate after);

    List<DailyMarketSignalDto> getMonthlyMarketSignalsByCompanyId(int companyId, LocalDate after);

    List<DerivedMetricsDto> getDerivedMetricsByCompanyId(int companyId, LocalDate after);
}