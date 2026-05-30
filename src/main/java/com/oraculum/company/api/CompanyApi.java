package com.oraculum.company.api;

import com.oraculum.company.api.dto.*;

import java.util.List;

/**
 * Public API for the Company module.
 */
public interface CompanyApi {

    TickerDto getTicker(String ticker, String market);

    List<TickerDto> getAllTickers();

    List<MarketDto> getAllMarkets();

    List<IndustryDto> getAllIndustries();

    List<NewsTickerDto> getNewsByTicker(String ticker, int days, int limit);

    List<BalanceSheetDto> getBalanceSheetsByCompanyId(String ticker, String variant, int limit);

    List<CashFlowStatementDto> getCashFlowStatementsByTicker(String ticker, String variant, int limit);

    List<IncomeStatementDto> getIncomeStatementsByTicker(String ticker, String variant, int limit);

    List<SharePriceDto> getSharePricesByTicker(String ticker);

    List<DailyMarketSignalDto> getDailyMarketSignalsByTicker(String ticker);

    List<DerivedMetricsDto> getDerivedMetricsByTicker(String ticker);
}