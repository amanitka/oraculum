package com.oraculum.analyst.service;

import com.oraculum.analyst.config.AnalystProperties;
import com.oraculum.analyst.dto.CitationRegistry;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.company.api.*;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.domain.TickerDocumentSubtype;
import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.company.api.dto.*;
import com.oraculum.economy.api.EconomyDataApi;
import com.oraculum.harvester.api.HarvesterLiveApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyFactSheetDataService {

    private final CompanyFinancialDataApi companyFinancialDataApi;
    private final CompanySharePriceApi companySharePriceApi;
    private final CompanyNewsApi companyNewsApi;
    private final CompanyInsiderTransactionApi companyInsiderTransactionApi;
    private final HarvesterLiveApi harvesterLiveApi;
    private final EconomyDataApi economyDataApi;
    private final CompanyTickerDocumentApi companyTickerDocumentApi;
    private final JsonMapper jsonMapper;
    private final AnalystProperties analystProperties;
    private final CompanyValuationApi companyValuationApi;

    private Map<StatementVariant, List<IncomeStatementDto>> getIncomeStatements(CompanyDto company, LocalDate annualAfter, LocalDate quarterlyAfter) {
        return companyFinancialDataApi.getIncomeStatementsByCompanyId(company.id(), annualAfter)
                .stream()
                .filter(dto -> dto.variant() == StatementVariant.ANNUAL || !dto.reportDate().isBefore(quarterlyAfter))
                .collect(Collectors.groupingBy(IncomeStatementDto::variant));
    }

    private Map<StatementVariant, List<BalanceSheetDto>> getBalanceSheets(CompanyDto company, LocalDate annualAfter, LocalDate quarterlyAfter) {
        return companyFinancialDataApi.getBalanceSheetsByCompanyId(company.id(), annualAfter)
                .stream()
                .filter(dto -> dto.variant() == StatementVariant.ANNUAL || !dto.reportDate().isBefore(quarterlyAfter))
                .collect(Collectors.groupingBy(BalanceSheetDto::variant));
    }

    private Map<StatementVariant, List<CashFlowStatementDto>> getCashFlowStatements(CompanyDto company,
                                                                                    LocalDate annualAfter, LocalDate quarterlyAfter) {
        return companyFinancialDataApi.getCashFlowStatementsByCompanyId(company.id(), annualAfter)
                .stream()
                .filter(dto -> dto.variant() == StatementVariant.ANNUAL || !dto.reportDate().isBefore(quarterlyAfter))
                .collect(Collectors.groupingBy(CashFlowStatementDto::variant));
    }

    private Map<StatementVariant, List<CompanyFinancialRatiosDto>> getCompanyFinancialRatios(CompanyDto company,
                                                                                             LocalDate annualAfter, LocalDate quarterlyAfter) {
        return companyFinancialDataApi.getCompanyFinancialRatiosByCompanyId(company.id(), annualAfter)
                .stream()
                .filter(dto -> dto.variant() == StatementVariant.ANNUAL || !dto.reportDate().isBefore(quarterlyAfter))
                .collect(Collectors.groupingBy(CompanyFinancialRatiosDto::variant));
    }

    private Map<StatementVariant, List<IndustryFinancialRatiosDto>> getIndustryFinancialRatios(CompanyDto company) {
        if (company.industryName() == null || company.industryName().isBlank()) {
            return Map.of();
        }
        return companyFinancialDataApi.getIndustryFinancialRatiosByIndustryName(company.industryName())
                .stream()
                .collect(Collectors.groupingBy(IndustryFinancialRatiosDto::variant));
    }

    private List<SharePriceSignalDto> getDailySharePriceSignals(CompanyDto company, LocalDate after) {
        return companySharePriceApi.getDailySharePriceSignalsByCompanyId(company.id(), after);
    }

    private List<SharePriceSignalDto> getMonthlySharePriceSignals(CompanyDto company, LocalDate after) {
        return companySharePriceApi.getMonthlySharePriceSignalsByCompanyId(company.id(), after);
    }

    private List<NewsTickerDto> getNews(CompanyDto company, LocalDate after, int limit) {
        List<NewsTickerDto> news = companyNewsApi.getNewsByTicker(company.ticker(), after);
        if (news == null || news.isEmpty()) return news;

        return news.stream()
                .sorted(Comparator.comparing(NewsTickerDto::relevanceScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(NewsTickerDto::timePublished, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit > 0 ? limit : 50)
                .sorted(Comparator.comparing(NewsTickerDto::timePublished, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private TickerNewsSentimentDto getNewsSentiment(CompanyDto company) {
        return companyNewsApi.getNewsSentimentByTicker(company.ticker()).orElse(null);
    }

    private InsiderTransactionSummaryDto getInsiderTransactionSummary(CompanyDto company) {
        return companyInsiderTransactionApi.getInsiderTransactionSummaryByTicker(company.ticker()).orElse(null);
    }

    private List<InsiderTransactionTickerDto> getRecentInsiderTransactions(CompanyDto company, LocalDate after) {
        return companyInsiderTransactionApi.getInsiderTransactionsByTicker(company.ticker(), after);
    }

    private Map<TickerDocumentType, Map<TickerDocumentSubtype, List<TickerDocumentDto>>> getRecentSecDocuments(CompanyDto company) {
        TickerKeyDto key = new TickerKeyDto(company.ticker(), company.market());
        List<TickerDocumentDto> documents = companyTickerDocumentApi.getDocumentsForAnalysisByTicker(key);
        Map<TickerDocumentType, Map<TickerDocumentSubtype, List<TickerDocumentDto>>> result = new EnumMap<>(TickerDocumentType.class);

        for (TickerDocumentDto doc : documents) {
            TickerDocumentType type = doc.getDocumentType();
            TickerDocumentSubtype subtype = doc.getDocumentSubtype();
            if (subtype == null) continue;

            Map<TickerDocumentSubtype, List<TickerDocumentDto>> subMap = result.computeIfAbsent(type, _ -> new EnumMap<>(TickerDocumentSubtype.class));
            List<TickerDocumentDto> list = subMap.computeIfAbsent(subtype, _ -> new ArrayList<>());
            list.add(doc);
        }
        return result;
    }

    public CompanyFactSheetData create(CompanyDto company, CitationRegistry citationRegistry) {
        LocalDate annualAfter = analystProperties.factSheet().getAnnualFactSheetHistoryDate();
        LocalDate quarterlyAfter = analystProperties.factSheet().getQuarterlyFactSheetHistoryDate();
        List<SharePriceSignalDto> dailySignals = getDailySharePriceSignals(company, analystProperties.sharePrice().getSharePriceHistoryDate());
        List<SharePriceSignalDto> monthlySignals = getMonthlySharePriceSignals(company, analystProperties.sharePrice().getMonthlySharePriceHistoryDate());
        Map<StatementVariant, List<CompanyFinancialRatiosDto>> ratios = getCompanyFinancialRatios(company, annualAfter, quarterlyAfter);

        return CompanyFactSheetData.builder()
                .jsonMapper(jsonMapper)
                .company(company)
                .citationRegistry(citationRegistry)
                .incomeStatements(getIncomeStatements(company, annualAfter, quarterlyAfter))
                .balanceSheets(getBalanceSheets(company, annualAfter, quarterlyAfter))
                .cashFlowStatements(getCashFlowStatements(company, annualAfter, quarterlyAfter))
                .companyFinancialRatios(ratios)
                .industryFinancialRatios(getIndustryFinancialRatios(company))
                .dailySharePriceSignals(dailySignals)
                .monthlySharePriceSignals(monthlySignals)
                .recentNews(getNews(company, analystProperties.news().getNewsHistoryDate(), analystProperties.news().articleLimit()))
                .newsSentimentAggregate(getNewsSentiment(company))
                .insiderTransactionSummary(getInsiderTransactionSummary(company))
                .recentInsiderTransactions(getRecentInsiderTransactions(company, analystProperties.insider().getTransactionHistoryDate()))
                .earningsEstimates(harvesterLiveApi.fetchEarningsEstimates(company.ticker()).orElse(List.of()))
                .macroeconomicSummary(economyDataApi.getMacroeconomicSummary())
                .recentSecDocuments(getRecentSecDocuments(company))
                .reverseDcfResult(companyValuationApi.calculateReverseDcf(company.id()))
                .historicalValuationPercentiles(companyValuationApi.calculateHistoricalValuationPercentiles(company.id()))
                .build();
    }

}
