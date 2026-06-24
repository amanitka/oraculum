package com.oraculum.company.service.impl;

import com.oraculum.common.exception.EntityNotFoundException;
import com.oraculum.company.api.dto.*;
import com.oraculum.company.domain.CompanyEntity;
import com.oraculum.company.domain.IndustryFinancialRatiosRepository;
import com.oraculum.company.domain.NewsEntity;
import com.oraculum.company.domain.NewsTickerEntity;
import com.oraculum.company.repository.*;
import com.oraculum.company.service.CompanyService;
import com.oraculum.company.service.mapper.NewsArticleMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final IndustryFinancialRatiosRepository industryFinancialRatiosRepository;
    private final InsiderTransactionTickerRepository insiderTransactionTickerRepository;
    private final ScreenerMasterRepository screenerMasterRepository;
    private final ScreenerNewsSentimentRepository screenerNewsSentimentRepository;
    private final ScreenerUndervaluedRepository screenerUndervaluedRepository;
    private final ScreenerQualityCompoundersRepository screenerQualityCompoundersRepository;
    private final ScreenerGrahamDeepValueRepository screenerGrahamDeepValueRepository;
    private final ScreenerFinancialTrendRepository ScreenerFinancialTrendRepository;
    private final ScreenerInsiderRepository screenerInsiderRepository;
    private final InsiderTransactionSummaryRepository insiderTransactionSummaryRepository;
    private final TickerNewsSentimentRepository tickerNewsSentimentRepository;
    private final NewsArticleMapper newsArticleMapper;

    @Override
    public CompanyDto getCompanyById(int companyId) {
        return companyRepository.findById(companyId)
                .map(CompanyDto::fromEntity)
                .orElseThrow(() -> new EntityNotFoundException(CompanyEntity.class, String.valueOf(companyId)));
    }

    @Override
    public List<CompanyDto> getAllCompanies() {
        return companyRepository.findAll().stream()
                .sorted(Comparator.comparing(CompanyEntity::getTicker))
                .map(CompanyDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<String> getAllMarketIds() {
        return marketRepository.findAllMarketIds();
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
    public Optional<TickerNewsSentimentDto> getNewsSentimentByTicker(String ticker) {
        return tickerNewsSentimentRepository.findByTicker(ticker).map(TickerNewsSentimentDto::fromEntity);
    }

    @Override
    public List<BalanceSheetDto> getBalanceSheetsByCompanyId(int companyId, LocalDate after) {
        return balanceSheetRepository.findByCompanyIdAndReportDateAfter(companyId, after)
                .stream()
                .map(BalanceSheetDto::fromEntity)
                .sorted(Comparator.comparing(BalanceSheetDto::fiscalYear, Comparator.reverseOrder()).thenComparing(BalanceSheetDto::fiscalPeriod, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public List<CashFlowStatementDto> getCashFlowStatementsByCompanyId(int companyId, LocalDate after) {
        return cashFlowStatementRepository.findByCompanyIdAndReportDateAfter(companyId, after)
                .stream()
                .map(CashFlowStatementDto::fromEntity)
                .sorted(Comparator.comparing(CashFlowStatementDto::fiscalYear, Comparator.reverseOrder()).thenComparing(CashFlowStatementDto::fiscalPeriod, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    public List<IncomeStatementDto> getIncomeStatementsByCompanyId(int companyId, LocalDate after) {
        return incomeStatementRepository.findByCompanyIdAndReportDateAfter(companyId, after)
                .stream()
                .map(IncomeStatementDto::fromEntity)
                .sorted(Comparator.comparing(IncomeStatementDto::fiscalYear, Comparator.reverseOrder()).thenComparing(IncomeStatementDto::fiscalPeriod, Comparator.reverseOrder()))
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
    public Optional<LocalDate> getSharePricesLastTradeDate() {
        return sharePriceRepository.findMaxTradeDate();
    }

    @Override
    public Optional<LocalDateTime> getInsiderTransactionsLastFilingDate() {
        return insiderTransactionTickerRepository.findMaxFilingDate();
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
    public List<IndustryFinancialRatiosDto> getIndustryFinancialRatiosByIndustryName(String industryName, LocalDate after) {
        return industryFinancialRatiosRepository.findByIndustryName(industryName).stream()
                // Assuming we want all available periods, but typically we might filter by year if needed
                // Currently view doesn't easily filter by "after" without joining back to report dates, so we return all
                .map(IndustryFinancialRatiosDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<ScreenerMasterDto> getMasterScreener() {
        return screenerMasterRepository.findAll()
                .stream()
                .map(ScreenerMasterDto::fromEntity)
                .sorted(Comparator.comparing(ScreenerMasterDto::qualityRank, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public List<ScreenerNewsSentimentDto> getNewsSentimentScreener() {
        return screenerNewsSentimentRepository.findAll()
                .stream()
                .map(ScreenerNewsSentimentDto::fromEntity)
                .sorted(Comparator.comparing(ScreenerNewsSentimentDto::newsSentiment30d, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public List<ScreenerDto> getUndervaluedScreener() {
        return screenerUndervaluedRepository.findAll()
                .stream()
                .map(ScreenerDto::fromEntity)
                .sorted(Comparator.comparing(ScreenerDto::qualityScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public List<ScreenerDto> getQualityCompoundersScreener() {
        return screenerQualityCompoundersRepository.findAll()
                .stream()
                .map(ScreenerDto::fromEntity)
                .sorted(Comparator.comparing(ScreenerDto::qualityScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public List<ScreenerDto> getGrahamDeepValueScreener() {
        return screenerGrahamDeepValueRepository.findAll()
                .stream()
                .map(ScreenerDto::fromEntity)
                .sorted(Comparator.comparing(ScreenerDto::financialTrendScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public List<ScreenerDto> getFinancialTrendScreener() {
        return ScreenerFinancialTrendRepository.findAll()
                .stream()
                .map(ScreenerDto::fromEntity)
                .sorted(Comparator.comparing(ScreenerDto::financialTrendScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public List<ScreenerInsiderDto> getInsiderScreener() {
        return screenerInsiderRepository.findAll().stream()
                .map(ScreenerInsiderDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<InsiderTransactionSummaryDto> getInsiderTransactionSummaryByTicker(String ticker) {
        return insiderTransactionSummaryRepository.findById(ticker)
                .map(InsiderTransactionSummaryDto::fromEntity);
    }

    @Override
    public List<InsiderTransactionTickerDto> getInsiderTransactionsByTicker(String ticker, LocalDate after) {
        return insiderTransactionTickerRepository.findByTickerAndTradeDateAfterOrderByFilingDateDesc(ticker, after).stream()
                .map(InsiderTransactionTickerDto::fromEntity)
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
        newsRepository.saveAll(articles.stream().map(newsArticleMapper::toNewsEntity).collect(Collectors.toList()));
        newsTickerRepository.saveAll(articles.stream().flatMap(a -> newsArticleMapper.toNewsTickerEntities(a).stream()).collect(Collectors.toList()));
    }
}