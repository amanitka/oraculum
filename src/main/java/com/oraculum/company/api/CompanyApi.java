package com.oraculum.company.api;

import com.oraculum.company.api.dto.*;

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

    List<NewsTickerDto> getNewsByTicker(String ticker, int days, int limit);

    List<BalanceSheetDto> getBalanceSheetsByCompanyId(int companyId, String variant, int limit);

    List<CashFlowStatementDto> getCashFlowStatementsByCompanyId(int companyId, String variant, int limit);

    List<IncomeStatementDto> getIncomeStatementsByCompanyId(int companyId, String variant, int limit);

    List<SharePriceDto> getSharePricesByCompanyId(int companyId);

    List<DailyMarketSignalDto> getDailyMarketSignalsByCompanyId(int companyId);

    List<DerivedMetricsDto> getDerivedMetricsByCompanyId(int companyId);
}