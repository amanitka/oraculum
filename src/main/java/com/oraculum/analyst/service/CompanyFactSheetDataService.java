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
import com.oraculum.analyst.dto.ValuationScenarioDto;
import com.oraculum.harvester.api.dto.CompanyNewsDto;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
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
    private final CompanyInsiderTransactionApi companyInsiderTransactionApi;
    private final HarvesterLiveApi harvesterLiveApi;
    private final EconomyDataApi economyDataApi;
    private final CompanyTickerDocumentApi companyTickerDocumentApi;
    private final JsonMapper jsonMapper;
    private final AnalystProperties analystProperties;
    private final CompanyValuationApi companyValuationApi;
    private final CompanyInstitutionalFlowApi companyInstitutionalFlowApi;
    private final ValuationScenarioCalculator valuationScenarioCalculator;

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

    private InsiderTransactionSummaryDto getInsiderTransactionSummary(CompanyDto company) {
        return companyInsiderTransactionApi.getInsiderTransactionSummaryByTicker(company.ticker()).orElse(null);
    }

    private List<InsiderTransactionByPersonDto> getInsiderTransactionsByPerson(CompanyDto company) {
        return companyInsiderTransactionApi.getInsiderTransactionsByPerson(company.ticker());
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

    private List<CompanyNewsDto> getRecentNews(CompanyDto company) {
        return harvesterLiveApi.fetchCompanyNews(
                company.ticker(),
                analystProperties.news().getNewsHistoryDate(),
                LocalDate.now()
        );
    }

    public CompanyFactSheetData create(CompanyDto company, CitationRegistry citationRegistry) {
        LocalDate annualAfter = analystProperties.factSheet().getAnnualFactSheetHistoryDate();
        LocalDate quarterlyAfter = analystProperties.factSheet().getQuarterlyFactSheetHistoryDate();
        List<SharePriceSignalDto> dailySignals = getDailySharePriceSignals(company, analystProperties.sharePrice().getSharePriceHistoryDate());
        List<SharePriceSignalDto> monthlySignals = getMonthlySharePriceSignals(company, analystProperties.sharePrice().getMonthlySharePriceHistoryDate());
        Map<StatementVariant, List<CompanyFinancialRatiosDto>> ratios = getCompanyFinancialRatios(company, annualAfter, quarterlyAfter);
        List<EarningsEstimateDto> estimates = harvesterLiveApi.fetchEarningsEstimates(company.ticker()).orElse(List.of());
        ReverseDcfDto reverseDcf = companyValuationApi.calculateReverseDcf(company.id());
        Float latestPrice = (dailySignals != null && !dailySignals.isEmpty()) ? dailySignals.getFirst().sharePrice() : null;
        ValuationScenarioDto scenarios = valuationScenarioCalculator.calculate(latestPrice, estimates, LocalDate.now(), reverseDcf);

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
                .recentNews(getRecentNews(company))
                .insiderTransactionSummary(getInsiderTransactionSummary(company))
                .insiderTransactionsByPerson(getInsiderTransactionsByPerson(company))
                .earningsEstimates(estimates)
                .macroeconomicSummary(economyDataApi.getMacroeconomicSummary())
                .recentSecDocuments(getRecentSecDocuments(company))
                .reverseDcfResult(reverseDcf)
                .historicalValuationPercentiles(companyValuationApi.calculateHistoricalValuationPercentiles(company.id()))
                .institutionalFlowHistory(getInstitutionalFlowHistory(company))
                .allSectorsInstitutionalFlow(getAllSectorsInstitutionalFlow())
                .valuationScenarios(scenarios)
                .build();
    }

    private List<CompanyInstitutionalFlowDto> getInstitutionalFlowHistory(CompanyDto company) {
        if (company == null || company.id() == null) return List.of();
        return companyInstitutionalFlowApi.getInstitutionalFlowHistory(company.id());
    }

    private List<SectorInstitutionalFlowTrendDto> getAllSectorsInstitutionalFlow() {
        List<SectorInstitutionalFlowTrendDto> sectorFlows = companyInstitutionalFlowApi.getSectorInstitutionalFlowByPeriod(null);
        if (sectorFlows == null || sectorFlows.isEmpty()) return List.of();
        return sectorFlows.stream()
                .sorted(Comparator.comparing(SectorInstitutionalFlowTrendDto::flowPct).reversed())
                .toList();
    }

}
