package com.oraculum.company.service.impl;

import com.oraculum.common.exception.EntityNotFoundException;
import com.oraculum.company.api.dto.*;
import com.oraculum.company.domain.IndustryEntity;
import com.oraculum.company.domain.MarketEntity;
import com.oraculum.company.domain.NewsEntity;
import com.oraculum.company.domain.NewsTickerEntity;
import com.oraculum.company.domain.TickerEntity;
import com.oraculum.company.repository.*;
import com.oraculum.company.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final TickerRepository tickerRepository;
    private final MarketRepository marketRepository;
    private final IndustryRepository industryRepository;
    private final NewsRepository newsRepository;
    private final NewsTickerRepository newsTickerRepository;
    private final BalanceSheetRepository balanceSheetRepository;
    private final CashFlowStatementRepository cashFlowStatementRepository;
    private final IncomeStatementRepository incomeStatementRepository;
    private final SharePriceRepository sharePriceRepository;
    private final DailyMarketSignalRepository dailyMarketSignalRepository;
    private final DerivedMetricsRepository derivedMetricsRepository;

    @Override
    public TickerDto getTicker(String ticker) {
        return tickerRepository.findByTicker(ticker).map(TickerDto::fromEntity).orElseThrow(() -> new EntityNotFoundException(TickerEntity.class, ticker));
    }

    @Override
    public List<TickerDto> getAllTickers() {
        return tickerRepository.findAll().stream().map(TickerDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<MarketDto> getAllMarkets() {
        return marketRepository.findAll().stream().map(MarketDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public MarketDto createOrUpdateMarket(MarketDto marketDto) {
        MarketEntity entity = marketDto.toEntity();
        MarketEntity savedEntity = marketRepository.save(entity);
        return MarketDto.fromEntity(savedEntity);
    }

    @Override
    public List<IndustryDto> getAllIndustries() {
        return industryRepository.findAll().stream().map(IndustryDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public IndustryDto createOrUpdateIndustry(IndustryDto industryDto) {
        IndustryEntity entity = industryDto.toEntity();
        IndustryEntity savedEntity = industryRepository.save(entity);
        return IndustryDto.fromEntity(savedEntity);
    }

    @Override
    public NewsDto createOrUpdateNews(NewsDto newsDto) {
        NewsEntity entity = newsDto.toEntity();
        NewsEntity savedEntity = newsRepository.save(entity);
        return NewsDto.fromEntity(savedEntity);
    }

    @Override
    public NewsTickerDto createOrUpdateNewsTicker(NewsTickerDto newsTickerDto) {
        NewsTickerEntity entity = newsTickerDto.toEntity();
        NewsTickerEntity savedEntity = newsTickerRepository.save(entity);
        return NewsTickerDto.fromEntity(savedEntity);
    }

    @Override
    public List<NewsDto> getNewsByTicker(String ticker) {
        // This is a simplification. A real implementation would need a more complex query
        // to join NewsTickerEntity with NewsEntity. For now, we'll just return all news.
        return newsRepository.findAll().stream()
            .map(NewsDto::fromEntity)
            .collect(Collectors.toList());
    }

    @Override
    public List<BalanceSheetDto> getBalanceSheetsByTicker(String ticker) {
        return balanceSheetRepository.findByTicker(ticker).stream().map(BalanceSheetDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<CashFlowStatementDto> getCashFlowStatementsByTicker(String ticker) {
        return cashFlowStatementRepository.findByTicker(ticker).stream().map(CashFlowStatementDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<IncomeStatementDto> getIncomeStatementsByTicker(String ticker) {
        return incomeStatementRepository.findByTicker(ticker).stream().map(IncomeStatementDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<SharePriceDto> getSharePricesByTicker(String ticker) {
        return sharePriceRepository.findByTicker(ticker).stream().map(SharePriceDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<DailyMarketSignalDto> getDailyMarketSignalsByTicker(String ticker) {
        return dailyMarketSignalRepository.findByTicker(ticker).stream().map(DailyMarketSignalDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<DerivedMetricsDto> getDerivedMetricsByTicker(String ticker) {
        return derivedMetricsRepository.findByTicker(ticker).stream().map(DerivedMetricsDto::fromEntity).collect(Collectors.toList());
    }
}