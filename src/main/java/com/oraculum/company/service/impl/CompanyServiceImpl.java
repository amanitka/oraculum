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
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final SharePriceSignalRepository sharePriceSignalRepository;
    private final CompanyFinancialRatiosRepository companyFinancialRatiosRepository;

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
    public List<CompanyDto> getCompaniesByMarket(String market) {
        return companyRepository.findByMarket(market).stream().map(CompanyDto::fromEntity).collect(Collectors.toList());
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
    public Optional<OffsetDateTime> getNewsLastTimePublished() {
        return newsRepository.findMaxTimePublished();
    }

    @Override
    public List<NewsTickerDto> getNewsByTicker(String ticker, LocalDate after) {
        OffsetDateTime afterDateTime = after.atStartOfDay().atOffset(ZoneOffset.UTC);
        List<NewsTickerEntity> tickerRows = newsTickerRepository.findByTickerAndTimePublishedAfter(ticker, afterDateTime);
        List<String> newsIds = tickerRows.stream().map(NewsTickerEntity::getNewsId).collect(Collectors.toList());
        Map<String, NewsEntity> newsById = newsRepository.findByIdIn(newsIds)
                .stream()
                .collect(Collectors.toMap(NewsEntity::getId, Function.identity()));
        return tickerRows.stream()
                .filter(t -> newsById.containsKey(t.getNewsId()))
                .map(t -> NewsTickerDto.from(newsById.get(t.getNewsId()), t))
                .sorted(Comparator.comparing(NewsTickerDto::timePublished).reversed()) // Sort by timePublished descending
                .collect(Collectors.toList());
    }

    @Override
    public List<BalanceSheetDto> getBalanceSheetsByCompanyId(int companyId, LocalDate after) {
        return balanceSheetRepository.findByCompanyIdAndReportDateAfter(companyId, after)
                .stream()
                .map(BalanceSheetDto::fromEntity)
                .sorted(Comparator.comparing(BalanceSheetDto::reportDate).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<CashFlowStatementDto> getCashFlowStatementsByCompanyId(int companyId, LocalDate after) {
        return cashFlowStatementRepository.findByCompanyIdAndReportDateAfter(companyId, after)
                .stream()
                .map(CashFlowStatementDto::fromEntity)
                .sorted(Comparator.comparing(CashFlowStatementDto::reportDate).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<IncomeStatementDto> getIncomeStatementsByCompanyId(int companyId, LocalDate after) {
        return incomeStatementRepository.findByCompanyIdAndReportDateAfter(companyId, after)
                .stream()
                .map(IncomeStatementDto::fromEntity)
                .sorted(Comparator.comparing(IncomeStatementDto::reportDate).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<SharePriceDto> getSharePricesByCompanyId(int companyId, LocalDate after) {
        return sharePriceRepository.findByCompanyIdAndTradeDateAfter(companyId, after)
                .stream()
                .map(SharePriceDto::fromEntity)
                .sorted(Comparator.comparing(SharePriceDto::tradeDate).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<SharePriceDto> getMonthlySharePricesByCompanyId(int companyId, LocalDate after) {
        return sharePriceRepository.findByCompanyIdAndTradeDateAfterAndFlagLastDayOfMonth(companyId, after, "Y")
                .stream()
                .map(SharePriceDto::fromEntity)
                .sorted(Comparator.comparing(SharePriceDto::tradeDate).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<SharePriceSignalDto> getDailySharePriceSignalsByCompanyId(int companyId, LocalDate after) {
        return sharePriceSignalRepository.findByCompanyIdAndTradeDateAfter(companyId, after)
                .stream()
                .map(SharePriceSignalDto::fromEntity)
                .sorted(Comparator.comparing(SharePriceSignalDto::tradeDate).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<SharePriceSignalDto> getMonthlySharePriceSignalsByCompanyId(int companyId, LocalDate after) {
        return sharePriceSignalRepository.findByCompanyIdAndTradeDateAfterAndFlagLastDayOfMonth(companyId, after, "Y")
                .stream()
                .map(SharePriceSignalDto::fromEntity)
                .sorted(Comparator.comparing(SharePriceSignalDto::tradeDate).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<CompanyFinancialRatiosDto> getCompanyFinancialRatiosByCompanyId(int companyId, LocalDate after) {
        return companyFinancialRatiosRepository.findByCompanyIdAndReportDateAfter(companyId, after)
                .stream()
                .map(CompanyFinancialRatiosDto::fromEntity)
                .sorted(Comparator.comparing(CompanyFinancialRatiosDto::reportDate).reversed())
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
        newsTickerRepository.saveAll(articles.stream().flatMap(a -> a.toNewsTickerEntities().stream()).collect(Collectors.toList()));
    }
}