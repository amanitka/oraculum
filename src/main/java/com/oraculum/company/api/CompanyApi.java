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

    List<CompanyDto> getAllCompanies();

    List<String> getAllMarketIds();

    Optional<OffsetDateTime> getNewsLastTimePublished();

    List<NewsTickerDto> getNewsByTicker(String ticker, LocalDate after);

    Optional<TickerNewsSentimentDto> getNewsSentimentByTicker(String ticker);

    List<BalanceSheetDto> getBalanceSheetsByCompanyId(int companyId, LocalDate after);

    List<CashFlowStatementDto> getCashFlowStatementsByCompanyId(int companyId, LocalDate after);

    List<IncomeStatementDto> getIncomeStatementsByCompanyId(int companyId, LocalDate after);

    List<SharePriceDto> getSharePricesByCompanyId(int companyId, LocalDate after);

    Optional<LocalDate> getSharePricesLastTradeDate();

    List<SharePriceSignalDto> getDailySharePriceSignalsByCompanyId(int companyId, LocalDate after);

    List<SharePriceSignalDto> getMonthlySharePriceSignalsByCompanyId(int companyId, LocalDate after);

    List<CompanyFinancialRatiosDto> getCompanyFinancialRatiosByCompanyId(int companyId, LocalDate after);

    List<IndustryFinancialRatiosDto> getIndustryFinancialRatiosByIndustryName(String industryName, LocalDate after);

    List<ScreenerMasterDto> getMasterScreener();

    List<ScreenerNewsSentimentDto> getNewsSentimentScreener();

    List<ScreenerDto> getUndervaluedScreener();

    List<ScreenerDto> getQualityCompoundersScreener();

    List<ScreenerDto> getGrahamDeepValueScreener();

    List<ScreenerDto> getFinancialTrendScreener();
}