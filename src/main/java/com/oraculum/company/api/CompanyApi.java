package com.oraculum.company.api;

import com.oraculum.company.api.dto.*;

import java.util.List;

/**
 * Public API for the Company module.
 */
public interface CompanyApi {

    TickerDto getTicker(String ticker);

    List<TickerDto> getAllTickers();

    List<MarketDto> getAllMarkets();

    MarketDto createOrUpdateMarket(MarketDto marketDto);

    List<IndustryDto> getAllIndustries();

    IndustryDto createOrUpdateIndustry(IndustryDto industryDto);

    NewsDto createOrUpdateNews(NewsDto newsDto);

    NewsTickerDto createOrUpdateNewsTicker(NewsTickerDto newsTickerDto);

    List<NewsDto> getNewsByTicker(String ticker);

    List<BalanceSheetDto> getBalanceSheetsByTicker(String ticker);

    List<CashFlowStatementDto> getCashFlowStatementsByTicker(String ticker);

    List<IncomeStatementDto> getIncomeStatementsByTicker(String ticker);

    List<SharePriceDto> getSharePricesByTicker(String ticker);

    List<DailyMarketSignalDto> getDailyMarketSignalsByTicker(String ticker);

    List<DerivedMetricsDto> getDerivedMetricsByTicker(String ticker);
}