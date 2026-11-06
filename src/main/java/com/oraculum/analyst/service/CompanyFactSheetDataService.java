package com.oraculum.analyst.service;

import com.oraculum.analyst.config.AnalystProperties;
import com.oraculum.analyst.dto.CitationRegistry;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.analyst.dto.valuation.ValuationScenarioDto;
import com.oraculum.analyst.dto.valuation.ValuationScenarioRequest;
import com.oraculum.analyst.service.valuation.ValuationScenarioCalculator;
import com.oraculum.company.api.CompanyFinancialDataApi;
import com.oraculum.company.api.CompanyInsiderTransactionApi;
import com.oraculum.company.api.CompanyInstitutionalFlowApi;
import com.oraculum.company.api.CompanySharePriceApi;
import com.oraculum.company.api.CompanyTickerDocumentApi;
import com.oraculum.company.api.CompanyValuationApi;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.domain.TickerDocumentSubtype;
import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.company.api.dto.BalanceSheetDto;
import com.oraculum.company.api.dto.CashFlowStatementDto;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.company.api.dto.CompanyInstitutionalFlowDto;
import com.oraculum.company.api.dto.HistoricalValuationSummaryDto;
import com.oraculum.company.api.dto.IncomeStatementDto;
import com.oraculum.company.api.dto.IndustryFinancialRatiosDto;
import com.oraculum.company.api.dto.InsiderTransactionByPersonDto;
import com.oraculum.company.api.dto.InsiderTransactionSummaryDto;
import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.company.api.dto.SectorInstitutionalFlowTrendDto;
import com.oraculum.company.api.dto.SharePriceSignalDto;
import com.oraculum.company.api.dto.TickerDocumentDto;
import com.oraculum.company.api.dto.TickerKeyDto;
import com.oraculum.economy.api.EconomyDataApi;
import com.oraculum.harvester.api.HarvesterLiveApi;
import com.oraculum.harvester.api.dto.CompanyNewsDto;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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

    public CompanyFactSheetData create(CompanyDto company, CitationRegistry citationRegistry) {
        LocalDate annualAfter = analystProperties.factSheet().getAnnualFactSheetHistoryDate();
        LocalDate quarterlyAfter = analystProperties.factSheet().getQuarterlyFactSheetHistoryDate();
        List<SharePriceSignalDto> dailySignals = getDailySharePriceSignals(company, analystProperties.sharePrice().getSharePriceHistoryDate());
        Map<StatementVariant, List<CompanyFinancialRatiosDto>> ratios = getCompanyFinancialRatios(company, annualAfter, quarterlyAfter);
        List<EarningsEstimateDto> estimates = harvesterLiveApi.fetchEarningsEstimates(company.ticker()).orElse(List.of());
        ReverseDcfDto reverseDcf = companyValuationApi.calculateReverseDcf(company.id());
        List<HistoricalValuationSummaryDto> historicalValuation = companyValuationApi.calculateHistoricalValuationPercentiles(company.id());
        ValuationScenarioDto scenarios = buildValuationScenarios(dailySignals, ratios, estimates, reverseDcf, historicalValuation);

        CompanyFactSheetData.CompanyFactSheetDataBuilder builder = CompanyFactSheetData.builder()
                .jsonMapper(jsonMapper)
                .company(company)
                .citationRegistry(citationRegistry);
        populateFinancialAndMarketData(builder, company, annualAfter, quarterlyAfter, dailySignals, ratios);
        populateGovernanceAndValuationData(builder, company, estimates, reverseDcf, historicalValuation, scenarios);
        return builder.build();
    }

    private void populateFinancialAndMarketData(
            CompanyFactSheetData.CompanyFactSheetDataBuilder builder,
            CompanyDto company,
            LocalDate annualAfter,
            LocalDate quarterlyAfter,
            List<SharePriceSignalDto> dailySignals,
            Map<StatementVariant, List<CompanyFinancialRatiosDto>> ratios
    ) {
        builder.incomeStatements(getIncomeStatements(company, annualAfter, quarterlyAfter))
                .balanceSheets(getBalanceSheets(company, annualAfter, quarterlyAfter))
                .cashFlowStatements(getCashFlowStatements(company, annualAfter, quarterlyAfter))
                .companyFinancialRatios(ratios)
                .industryFinancialRatios(getIndustryFinancialRatios(company))
                .dailySharePriceSignals(dailySignals)
                .monthlySharePriceSignals(getMonthlySharePriceSignals(company, analystProperties.sharePrice().getMonthlySharePriceHistoryDate()))
                .recentNews(getRecentNews(company));
    }

    private void populateGovernanceAndValuationData(
            CompanyFactSheetData.CompanyFactSheetDataBuilder builder,
            CompanyDto company,
            List<EarningsEstimateDto> estimates,
            ReverseDcfDto reverseDcf,
            List<HistoricalValuationSummaryDto> historicalValuation,
            ValuationScenarioDto scenarios
    ) {
        builder.insiderTransactionSummary(getInsiderTransactionSummary(company))
                .insiderTransactionsByPerson(getInsiderTransactionsByPerson(company))
                .earningsEstimates(estimates)
                .macroeconomicSummary(economyDataApi.getMacroeconomicSummary())
                .recentSecDocuments(getRecentSecDocuments(company))
                .reverseDcfResult(reverseDcf)
                .historicalValuationPercentiles(historicalValuation)
                .institutionalFlowHistory(getInstitutionalFlowHistory(company))
                .allSectorsInstitutionalFlow(getAllSectorsInstitutionalFlow())
                .valuationScenarios(scenarios);
    }

    private ValuationScenarioDto buildValuationScenarios(
            List<SharePriceSignalDto> dailySignals,
            Map<StatementVariant, List<CompanyFinancialRatiosDto>> ratios,
            List<EarningsEstimateDto> estimates,
            ReverseDcfDto reverseDcf,
            List<HistoricalValuationSummaryDto> historicalValuation
    ) {
        if (valuationScenarioCalculator == null) {
            return null;
        }
        SharePriceSignalDto latestSignal = getLatestSignal(dailySignals);
        ValuationScenarioRequest request = ValuationScenarioRequest.builder()
                .currentSharePrice(latestSignal != null ? latestSignal.sharePrice() : null)
                .currentMarketCap(resolveLatestMarketCap(latestSignal, reverseDcf))
                .estimates(estimates)
                .analysisDate(LocalDate.now())
                .reverseDcfResult(reverseDcf)
                .quarterlyRatios(getQuarterlyRatios(ratios))
                .annualRatios(getAnnualRatios(ratios))
                .ttmRatio(getTtmRatio(ratios))
                .historicalValuation(historicalValuation)
                .build();
        return valuationScenarioCalculator.calculate(request);
    }

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

    private Map<StatementVariant, List<CashFlowStatementDto>> getCashFlowStatements(CompanyDto company, LocalDate annualAfter, LocalDate quarterlyAfter) {
        return companyFinancialDataApi.getCashFlowStatementsByCompanyId(company.id(), annualAfter)
                .stream()
                .filter(dto -> dto.variant() == StatementVariant.ANNUAL || !dto.reportDate().isBefore(quarterlyAfter))
                .collect(Collectors.groupingBy(CashFlowStatementDto::variant));
    }

    private Map<StatementVariant, List<CompanyFinancialRatiosDto>> getCompanyFinancialRatios(CompanyDto company, LocalDate annualAfter, LocalDate quarterlyAfter) {
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
            TickerDocumentSubtype subtype = doc.getDocumentSubtype();
            if (subtype == null) {
                continue;
            }
            result.computeIfAbsent(doc.getDocumentType(), _ -> new EnumMap<>(TickerDocumentSubtype.class))
                    .computeIfAbsent(subtype, _ -> new ArrayList<>())
                    .add(doc);
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

    private SharePriceSignalDto getLatestSignal(List<SharePriceSignalDto> dailySignals) {
        if (dailySignals == null || dailySignals.isEmpty()) {
            return null;
        }
        return dailySignals.stream()
                .filter(s -> s != null && s.tradeDate() != null)
                .max(Comparator.comparing(SharePriceSignalDto::tradeDate))
                .orElse(dailySignals.getFirst());
    }

    private Double resolveLatestMarketCap(SharePriceSignalDto latestSignal, ReverseDcfDto reverseDcf) {
        if (latestSignal != null && latestSignal.marketCapitalization() != null) {
            return latestSignal.marketCapitalization().doubleValue();
        }
        return reverseDcf != null ? reverseDcf.currentMarketCap() : null;
    }

    private List<CompanyFinancialRatiosDto> getQuarterlyRatios(Map<StatementVariant, List<CompanyFinancialRatiosDto>> ratios) {
        return ratios != null ? ratios.getOrDefault(StatementVariant.QUARTERLY, List.of()) : List.of();
    }

    private List<CompanyFinancialRatiosDto> getAnnualRatios(Map<StatementVariant, List<CompanyFinancialRatiosDto>> ratios) {
        return ratios != null ? ratios.getOrDefault(StatementVariant.ANNUAL, List.of()) : List.of();
    }

    private CompanyFinancialRatiosDto getTtmRatio(Map<StatementVariant, List<CompanyFinancialRatiosDto>> ratios) {
        if (ratios == null || ratios.get(StatementVariant.TTM) == null || ratios.get(StatementVariant.TTM).isEmpty()) {
            return null;
        }
        List<CompanyFinancialRatiosDto> ttmList = ratios.get(StatementVariant.TTM);
        return ttmList.stream()
                .filter(r -> r != null && r.reportDate() != null)
                .max(Comparator.comparing(CompanyFinancialRatiosDto::reportDate))
                .orElse(ttmList.getFirst());
    }

    private List<CompanyInstitutionalFlowDto> getInstitutionalFlowHistory(CompanyDto company) {
        if (company == null || company.id() == null) {
            return List.of();
        }
        List<CompanyInstitutionalFlowDto> raw = companyInstitutionalFlowApi.getInstitutionalFlowHistory(company.id());
        return propagateIncompleteSampleFlagToQoqDelta(raw);
    }

    private List<CompanyInstitutionalFlowDto> propagateIncompleteSampleFlagToQoqDelta(
            List<CompanyInstitutionalFlowDto> flows
    ) {
        if (flows == null || flows.size() < 2) {
            return flows != null ? flows : List.of();
        }
        List<CompanyInstitutionalFlowDto> chronologicallySorted = flows.stream()
                .filter(f -> f != null && f.reportPeriod() != null)
                .sorted(Comparator.comparing(CompanyInstitutionalFlowDto::reportPeriod))
                .toList();
        if (chronologicallySorted.size() < 2) {
            return flows;
        }
        List<CompanyInstitutionalFlowDto> adjusted = new ArrayList<>(chronologicallySorted.size());
        for (int i = 0; i < chronologicallySorted.size(); i++) {
            CompanyInstitutionalFlowDto current = chronologicallySorted.get(i);
            boolean priorIncomplete = i > 0 && Boolean.TRUE.equals(chronologicallySorted.get(i - 1).flagIncompleteFilerSample());
            boolean currentIncomplete = Boolean.TRUE.equals(current.flagIncompleteFilerSample());
            adjusted.add((currentIncomplete || priorIncomplete)
                    ? current.withFlagIncompleteFilerSample(true)
                    : current);
        }
        return adjusted.reversed();
    }

    private List<SectorInstitutionalFlowTrendDto> getAllSectorsInstitutionalFlow() {
        List<SectorInstitutionalFlowTrendDto> sectorFlows = companyInstitutionalFlowApi.getSectorInstitutionalFlowByPeriod(null);
        if (sectorFlows == null || sectorFlows.isEmpty()) {
            return List.of();
        }
        return sectorFlows.stream()
                .sorted(Comparator.comparing(SectorInstitutionalFlowTrendDto::flowPct).reversed())
                .toList();
    }
}
