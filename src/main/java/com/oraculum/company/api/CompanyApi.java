package com.oraculum.company.api;

import com.oraculum.company.api.dto.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Public API for the Company module.
 */
public interface CompanyApi {

    CompanyDto getCompanyById(int companyId);

    List<CompanyDto> getCompaniesByMarket(String market);

    List<MarketDto> getAllMarkets();

    List<String> getAllMarketIds();

    List<IndustryDto> getAllIndustries();

    Optional<OffsetDateTime> getNewsLastTimePublished();

    List<NewsTickerDto> getNewsByTicker(String ticker, LocalDate after);

    List<BalanceSheetDto> getBalanceSheetsByCompanyId(int companyId, LocalDate after);

    List<CashFlowStatementDto> getCashFlowStatementsByCompanyId(int companyId, LocalDate after);

    List<IncomeStatementDto> getIncomeStatementsByCompanyId(int companyId, LocalDate after);

    List<SharePriceDto> getSharePricesByCompanyId(int companyId, LocalDate after);

    List<SharePriceDto> getMonthlySharePricesByCompanyId(int companyId, LocalDate after);

    Optional<LocalDate> getSharePricesLastTradeDate();

    List<SharePriceSignalDto> getDailySharePriceSignalsByCompanyId(int companyId, LocalDate after);

    List<SharePriceSignalDto> getMonthlySharePriceSignalsByCompanyId(int companyId, LocalDate after);

    List<CompanyFinancialRatiosDto> getCompanyFinancialRatiosByCompanyId(int companyId, LocalDate after);
}