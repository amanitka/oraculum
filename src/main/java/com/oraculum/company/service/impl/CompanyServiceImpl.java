package com.oraculum.company.service.impl;

import com.oraculum.common.exception.EntityNotFoundException;
import com.oraculum.company.api.dto.*;
import com.oraculum.company.domain.CompanyEntity;
import com.oraculum.company.domain.NewsEntity;
import com.oraculum.company.domain.NewsTickerEntity;
import com.oraculum.company.repository.*;
import com.oraculum.company.service.CompanyService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
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
    public CompanyDto getCompany(String ticker, String market) {
        return companyRepository.findByTickerAndMarket(ticker, market)
                .map(CompanyDto::fromEntity)
                .orElseThrow(() -> new EntityNotFoundException(CompanyEntity.class, ticker + ":" + market));
    }

    @Override
    public CompanyDto getCompanyById(int companyId) {
        return companyRepository.findById(companyId)
                .map(CompanyDto::fromEntity)
                .orElseThrow(() -> new EntityNotFoundException(CompanyEntity.class, String.valueOf(companyId)));
    }

    @Override
    public List<CompanyDto> getAllCompanies() {
        return companyRepository.findAll().stream().map(CompanyDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<MarketDto> getAllMarkets() {
        return marketRepository.findAll().stream().map(MarketDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<IndustryDto> getAllIndustries() {
        return industryRepository.findAll().stream().map(IndustryDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<NewsTickerDto> getNewsByTicker(String ticker, int days, int limit) {
        OffsetDateTime after = OffsetDateTime.now().minusDays(days);
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timePublished"));
        Page<NewsTickerEntity> tickerRows = newsTickerRepository.findByTickerAndTimePublishedAfter(ticker,
                after,
                pageable);
        List<String> newsIds = tickerRows.stream().map(NewsTickerEntity::getNewsId).collect(Collectors.toList());
        Map<String, NewsEntity> newsById = newsRepository.findByIdIn(newsIds)
                .stream()
                .collect(Collectors.toMap(NewsEntity::getId, Function.identity()));
        return tickerRows.stream()
                .filter(t -> newsById.containsKey(t.getNewsId()))
                .map(t -> NewsTickerDto.from(newsById.get(t.getNewsId()), t))
                .collect(Collectors.toList());
    }

    @Override
    public List<BalanceSheetDto> getBalanceSheetsByCompanyId(int companyId, String variant, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "reportDate"));
        return balanceSheetRepository.findByCompanyIdAndVariant(companyId, variant, pageable)
                .stream()
                .map(BalanceSheetDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<CashFlowStatementDto> getCashFlowStatementsByCompanyId(int companyId, String variant, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "reportDate"));
        return cashFlowStatementRepository.findByCompanyIdAndVariant(companyId, variant, pageable)
                .stream()
                .map(CashFlowStatementDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<IncomeStatementDto> getIncomeStatementsByCompanyId(int companyId, String variant, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "reportDate"));
        return incomeStatementRepository.findByCompanyIdAndVariant(companyId, variant, pageable)
                .stream()
                .map(IncomeStatementDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<SharePriceDto> getSharePricesByCompanyId(int companyId) {
        return sharePriceRepository.findByCompanyId(companyId)
                .stream()
                .map(SharePriceDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<DailyMarketSignalDto> getDailyMarketSignalsByCompanyId(int companyId) {
        return dailyMarketSignalRepository.findByCompanyId(companyId)
                .stream()
                .map(DailyMarketSignalDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<DerivedMetricsDto> getDerivedMetricsByCompanyId(int companyId) {
        return derivedMetricsRepository.findByCompanyId(companyId)
                .stream()
                .map(DerivedMetricsDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void createOrUpdateMarket(MarketDto market) {
        marketRepository.save(market.toEntity());
    }

    @Override
    @Transactional
    public void createOrUpdateIndustry(IndustryDto industry) {
        industryRepository.save(industry.toEntity());
    }

    @Override
    @Transactional
    public void createOrUpdateNewsBatch(List<NewsArticleDto> articles) {
        newsRepository.saveAll(articles.stream().map(NewsArticleDto::toNewsEntity).collect(Collectors.toList()));
        newsTickerRepository.saveAll(articles.stream()
                .flatMap(a -> a.toNewsTickerEntities().stream())
                .collect(Collectors.toList()));
    }
}