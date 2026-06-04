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

    List<SharePriceSignalDto> getDailySharePriceSignalsByCompanyId(int companyId, LocalDate after);

    List<SharePriceSignalDto> getMonthlySharePriceSignalsByCompanyId(int companyId, LocalDate after);

    List<CompanyFinancialRatiosDto> getCompanyFinancialRatiosByCompanyId(int companyId, LocalDate after);
}