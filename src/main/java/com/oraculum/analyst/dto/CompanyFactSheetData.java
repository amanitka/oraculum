package com.oraculum.analyst.dto;

import com.oraculum.analyst.agent.dto.CanonicalFacts;
import com.oraculum.analyst.service.citation.FactSheetCitationFormatter;
import com.oraculum.analyst.util.JsonUtils;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.domain.TickerDocumentSubtype;
import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.company.api.dto.*;
import com.oraculum.economy.api.dto.MacroSummaryDto;
import com.oraculum.harvester.api.dto.CompanyNewsDto;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.*;

@Getter
@Slf4j
public class CompanyFactSheetData {

    private final JsonMapper jsonMapper;
    private final CitationRegistry citationRegistry;
    private final CompanyDto company;
    private final Map<StatementVariant, List<IncomeStatementDto>> incomeStatements;
    private final Map<StatementVariant, List<BalanceSheetDto>> balanceSheets;
    private final Map<StatementVariant, List<CashFlowStatementDto>> cashFlowStatements;
    private final Map<StatementVariant, List<CompanyFinancialRatiosDto>> companyFinancialRatios;
    private final Map<StatementVariant, List<IndustryFinancialRatiosDto>> industryFinancialRatios;
    private final List<SharePriceSignalDto> dailySharePriceSignals;
    private final List<SharePriceSignalDto> monthlySharePriceSignals;
    private final List<CompanyNewsDto> recentNews;
    private final InsiderTransactionSummaryDto insiderTransactionSummary;
    private final List<InsiderTransactionByPersonDto> insiderTransactionsByPerson;
    private final List<EarningsEstimateDto> earningsEstimates;
    private final List<MacroSummaryDto> macroeconomicSummary;
    private final Map<TickerDocumentType, Map<TickerDocumentSubtype, List<TickerDocumentDto>>> recentSecDocuments;
    private final ReverseDcfDto reverseDcfResult;
    private final ValuationScenarioDto valuationScenarios;
    private final List<HistoricalValuationSummaryDto> historicalValuationPercentiles;
    private final List<CompanyInstitutionalFlowDto> institutionalFlowHistory;
    private final List<SectorInstitutionalFlowTrendDto> allSectorsInstitutionalFlow;

    @Getter(AccessLevel.NONE)
    private final FactSheetCitationFormatter formatter;

    @Builder
    public CompanyFactSheetData(
            JsonMapper jsonMapper,
            CitationRegistry citationRegistry,
            CompanyDto company,
            Map<StatementVariant, List<IncomeStatementDto>> incomeStatements,
            Map<StatementVariant, List<BalanceSheetDto>> balanceSheets,
            Map<StatementVariant, List<CashFlowStatementDto>> cashFlowStatements,
            Map<StatementVariant, List<CompanyFinancialRatiosDto>> companyFinancialRatios,
            Map<StatementVariant, List<IndustryFinancialRatiosDto>> industryFinancialRatios,
            List<SharePriceSignalDto> dailySharePriceSignals,
            List<SharePriceSignalDto> monthlySharePriceSignals,
            List<CompanyNewsDto> recentNews,
            InsiderTransactionSummaryDto insiderTransactionSummary,
            List<InsiderTransactionByPersonDto> insiderTransactionsByPerson,
            List<EarningsEstimateDto> earningsEstimates,
            List<MacroSummaryDto> macroeconomicSummary,
            Map<TickerDocumentType, Map<TickerDocumentSubtype, List<TickerDocumentDto>>> recentSecDocuments,
            ReverseDcfDto reverseDcfResult,
            ValuationScenarioDto valuationScenarios,
            List<HistoricalValuationSummaryDto> historicalValuationPercentiles,
            List<CompanyInstitutionalFlowDto> institutionalFlowHistory,
            List<SectorInstitutionalFlowTrendDto> allSectorsInstitutionalFlow
    ) {
        this.jsonMapper = jsonMapper != null ? jsonMapper : new JsonMapper();
        this.citationRegistry = citationRegistry != null ? citationRegistry : new CitationRegistry();
        this.company = company;
        this.incomeStatements = incomeStatements;
        this.balanceSheets = balanceSheets;
        this.cashFlowStatements = cashFlowStatements;
        this.companyFinancialRatios = companyFinancialRatios;
        this.industryFinancialRatios = industryFinancialRatios;
        this.dailySharePriceSignals = dailySharePriceSignals;
        this.monthlySharePriceSignals = monthlySharePriceSignals;
        this.recentNews = recentNews;
        this.insiderTransactionSummary = insiderTransactionSummary;
        this.insiderTransactionsByPerson = insiderTransactionsByPerson;
        this.earningsEstimates = earningsEstimates;
        this.macroeconomicSummary = macroeconomicSummary;
        this.recentSecDocuments = recentSecDocuments;
        this.reverseDcfResult = reverseDcfResult;
        this.valuationScenarios = valuationScenarios;
        this.historicalValuationPercentiles = historicalValuationPercentiles;
        this.institutionalFlowHistory = institutionalFlowHistory;
        this.allSectorsInstitutionalFlow = allSectorsInstitutionalFlow;
        this.formatter = new FactSheetCitationFormatter(this.jsonMapper, this.citationRegistry);
    }

    public String getCompanyProfile() {
        return formatter.wrapWithCitation(CompanyDto.class, company != null ? company.id() : null, company);
    }

    public String getIncomeStatementHistory(StatementVariant variant) {
        return getIncomeStatementHistory(variant, Integer.MAX_VALUE);
    }

    public String getIncomeStatementHistory(StatementVariant variant, int limit) {
        return formatter.formatStatementHistory(
                IncomeStatementDto.class,
                incomeStatements != null ? incomeStatements.get(variant) : null,
                limit,
                IncomeStatementDto::id,
                IncomeStatementDto::statementData,
                "Income Statement",
                variant
        );
    }

    public String getBalanceSheetHistory(StatementVariant variant) {
        return getBalanceSheetHistory(variant, Integer.MAX_VALUE);
    }

    public String getBalanceSheetHistory(StatementVariant variant, int limit) {
        return formatter.formatStatementHistory(
                BalanceSheetDto.class,
                balanceSheets != null ? balanceSheets.get(variant) : null,
                limit,
                BalanceSheetDto::id,
                BalanceSheetDto::statementData,
                "Balance Sheet",
                variant
        );
    }

    public String getCashFlowHistory(StatementVariant variant, int limit) {
        return formatter.formatStatementHistory(
                CashFlowStatementDto.class,
                cashFlowStatements != null ? cashFlowStatements.get(variant) : null,
                limit,
                CashFlowStatementDto::id,
                CashFlowStatementDto::statementData,
                "Cash Flow Statement",
                variant
        );
    }

    public String getCompanyFinancialRatios(StatementVariant variant) {
        return getCompanyFinancialRatios(variant, Integer.MAX_VALUE);
    }

    public String getCompanyFinancialRatios(StatementVariant variant, int limit) {
        List<CompanyFinancialRatiosDto> dtos = companyFinancialRatios != null ? companyFinancialRatios.get(variant) : null;
        if (dtos == null || dtos.isEmpty()) {
            return "[]";
        }
        List<CompanyFinancialRatiosSlim> slimDtos = dtos.stream()
                .limit(limit)
                .map(dto -> {
                    Object payload = formatter.getCitationPayloadFromDto(dto, "Financial Ratios", variant);
                    String citationId = citationRegistry.getOrAssignCitationId(CompanyFinancialRatiosDto.class, dto.id(), payload);
                    return CompanyFinancialRatiosSlim.from(dto, citationId);
                })
                .toList();
        return JsonUtils.toJson(jsonMapper, slimDtos, "[]");
    }

    public List<CompanyFinancialRatiosDto> getCompanyFinancialRatiosList(StatementVariant variant) {
        if (companyFinancialRatios == null) {
            return List.of();
        }
        return Optional.ofNullable(companyFinancialRatios.get(variant)).orElse(List.of());
    }

    public String getCanonicalFacts() {
        CompanyFinancialRatiosDto ttm = getCompanyFinancialRatiosList(StatementVariant.TTM).stream()
                .max(Comparator.comparing(CompanyFinancialRatiosDto::reportDate))
                .orElse(null);
        CompanyFinancialRatiosDto annual = getCompanyFinancialRatiosList(StatementVariant.ANNUAL).stream()
                .max(Comparator.comparing(CompanyFinancialRatiosDto::reportDate))
                .orElse(null);

        if (ttm == null && company != null) {
            log.warn("No TTM ratios for {}. Canonical facts incomplete.", company.ticker());
        }

        SharePriceSignalDto latestSignal = getLatestDailySignal();
        LocalDate latestTradeDate = latestSignal != null ? latestSignal.tradeDate() : null;
        CanonicalFacts facts = CanonicalFacts.from(ttm, annual, latestTradeDate);
        return JsonUtils.toJson(jsonMapper, facts, "{}");
    }

    public String getDailySharePriceSignals() {
        return processSharePriceSignals(dailySharePriceSignals, Integer.MAX_VALUE);
    }

    public String getLatestDailySharePriceSignals(int limit) {
        return processSharePriceSignals(dailySharePriceSignals, limit);
    }

    public String getMonthlySharePriceSignals() {
        return processSharePriceSignals(monthlySharePriceSignals, Integer.MAX_VALUE);
    }

    private String processSharePriceSignals(List<SharePriceSignalDto> signals, int limit) {
        if (signals == null || signals.isEmpty()) return "[]";
        List<SharePriceSignalSlim> slim = signals.stream()
                .limit(limit)
                .map(dto -> {
                    SharePriceSignalSlim unCited = SharePriceSignalSlim.from(dto, null);
                    String citationId = citationRegistry.getOrAssignCitationId(SharePriceSignalSlim.class, dto.getId(), unCited);
                    return SharePriceSignalSlim.from(dto, citationId);
                })
                .toList();
        return JsonUtils.toJson(jsonMapper, slim, "[]");
    }

    public String getRecentCompanyNews(int limit) {
        return serializeFilteredNews(true, limit, CompanyNewsSlim.RELEVANCE_DIRECT);
    }

    public String getRecentMarketNews(int limit) {
        return serializeFilteredNews(false, limit, CompanyNewsSlim.RELEVANCE_MARKET_CONTEXT);
    }

    private String serializeFilteredNews(boolean directOnly, int limit, String relevance) {
        List<CompanyNewsDto> filtered = filterNews(directOnly, limit);
        if (filtered.isEmpty()) return "[]";
        List<CompanyNewsSlim> slim = filtered.stream()
                .map(dto -> {
                    String citationId = citationRegistry.getOrAssignCitationId(CompanyNewsDto.class, dto.id(), dto);
                    return CompanyNewsSlim.from(dto, citationId, relevance);
                })
                .toList();
        return JsonUtils.toJson(jsonMapper, slim, "[]");
    }

    private List<CompanyNewsDto> filterNews(boolean directOnly, int limit) {
        if (recentNews == null || recentNews.isEmpty() || limit <= 0) return List.of();
        String ticker = company != null ? company.ticker() : null;
        String compName = company != null ? company.companyName() : null;
        return recentNews.stream()
                .filter(dto -> directOnly == CompanyNewsSlim.isDirectCompanyNews(dto, ticker, compName))
                .limit(limit)
                .toList();
    }

    public List<CompanyNewsDto> getRecentNewsList() {
        return recentNews != null ? recentNews : List.of();
    }

    public SharePriceSignalDto getLatestDailySignal() {
        return (dailySharePriceSignals != null && !dailySharePriceSignals.isEmpty())
                ? dailySharePriceSignals.getFirst()
                : null;
    }

    public String getLatestIndustryRatios(StatementVariant variant) {
        if (industryFinancialRatios == null) return "[]";
        List<IndustryFinancialRatiosDto> ratios = industryFinancialRatios.get(variant);
        if (ratios == null || ratios.isEmpty()) return "[]";
        String key = (company != null ? company.industryName() : "") + "_" + variant;
        return formatter.wrapWithCitation(IndustryFinancialRatiosDto.class, key, ratios);
    }

    public String getInsiderTransactionSummary() {
        String key = company != null ? company.ticker() : null;
        return formatter.wrapWithCitation(InsiderTransactionSummaryDto.class, key, insiderTransactionSummary);
    }

    public InsiderTransactionSummaryDto getInsiderTransactionSummaryDto() {
        return insiderTransactionSummary;
    }

    public String getInsiderTransactionsByPerson() {
        return formatter.wrapListWithCitations(
                InsiderTransactionByPersonDto.class,
                insiderTransactionsByPerson,
                dto -> (company != null ? company.ticker() : "") + "_" + dto.insiderName()
        );
    }

    public List<InsiderTransactionByPersonDto> getInsiderTransactionsByPersonList() {
        return insiderTransactionsByPerson != null ? insiderTransactionsByPerson : List.of();
    }

    public String getFutureEarningsEstimates(LocalDate analysisDate) {
        if (earningsEstimates == null || earningsEstimates.isEmpty()) {
            return "[]";
        }
        List<EarningsEstimateDto> future = earningsEstimates.stream()
                .filter(est -> est.date() != null && est.date().isAfter(analysisDate))
                .toList();
        return formatter.wrapListWithCitations(EarningsEstimateDto.class, future, this::buildEarningsEstimateKey);
    }

    private String buildEarningsEstimateKey(EarningsEstimateDto dto) {
        String base = (company != null ? company.ticker() : "") + "_" + dto.date();
        if (dto.horizon() != null && !dto.horizon().isBlank()) {
            return base + "_" + dto.horizon().trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        }
        return base;
    }

    public List<EarningsEstimateDto> getEarningsEstimatesList() {
        return earningsEstimates != null ? earningsEstimates : List.of();
    }

    public String getMacroeconomicSummary() {
        return formatter.wrapListWithCitations(MacroSummaryDto.class, macroeconomicSummary, MacroSummaryDto::indicator);
    }

    public List<MacroSummaryDto> getMacroeconomicSummaryList() {
        return macroeconomicSummary != null ? macroeconomicSummary : List.of();
    }

    public List<SharePriceSignalDto> getDailySharePriceSignalsList() {
        return dailySharePriceSignals != null ? dailySharePriceSignals : List.of();
    }

    public String getInstitutionalFlowHistoryJson() {
        return formatter.wrapListWithCitations(
                CompanyInstitutionalFlowDto.class,
                institutionalFlowHistory,
                dto -> (company != null ? company.ticker() : "") + "_" + dto.reportPeriod()
        );
    }

    public String getSectorRotationSummaryJson() {
        return formatter.wrapListWithCitations(
                SectorInstitutionalFlowTrendDto.class,
                allSectorsInstitutionalFlow,
                dto -> dto.sectorName() + "_" + dto.reportPeriod()
        );
    }

    public String getRecentSecMdSummaries() {
        return getCombinedSecDocumentSummaries(TickerDocumentSubtype.SEC_MD);
    }

    public String getRecentSecRfSummaries() {
        return getCombinedSecDocumentSummaries(TickerDocumentSubtype.SEC_RF);
    }

    public String getRecentSecEx991Summaries() {
        return getSecDocumentSummaries(TickerDocumentType.SEC_8K, TickerDocumentSubtype.SEC_EX99_1);
    }

    public String getRecentSecEx991Summaries(LocalDate analysisDate, int maxAgeDays) {
        if (analysisDate == null || maxAgeDays <= 0) {
            return getRecentSecEx991Summaries();
        }
        LocalDate cutoff = analysisDate.minusDays(maxAgeDays);
        List<TickerDocumentDto> docs = Optional.ofNullable(recentSecDocuments)
                .map(m -> m.get(TickerDocumentType.SEC_8K))
                .map(m -> m.get(TickerDocumentSubtype.SEC_EX99_1))
                .orElse(List.of())
                .stream()
                .filter(doc -> isDocumentAfterCutoff(doc, cutoff))
                .toList();
        return convertDocsToJson(docs);
    }

    private boolean isDocumentAfterCutoff(TickerDocumentDto doc, LocalDate cutoff) {
        LocalDate effectiveDate = doc.getFilingDate() != null ? doc.getFilingDate() : doc.getReportPeriod();
        return effectiveDate != null && !effectiveDate.isBefore(cutoff);
    }

    private String getSecDocumentSummaries(TickerDocumentType type, TickerDocumentSubtype subtype) {
        List<TickerDocumentDto> docs = Optional.ofNullable(recentSecDocuments)
                .map(m -> m.get(type))
                .map(m -> m.get(subtype))
                .orElse(List.of());
        return convertDocsToJson(docs);
    }

    private String getCombinedSecDocumentSummaries(TickerDocumentSubtype subtype) {
        List<TickerDocumentDto> docs = new ArrayList<>();
        Optional.ofNullable(recentSecDocuments).ifPresent(m -> {
            Optional.ofNullable(m.get(TickerDocumentType.SEC_10K)).map(sub -> sub.get(subtype)).ifPresent(docs::addAll);
            Optional.ofNullable(m.get(TickerDocumentType.SEC_10Q)).map(sub -> sub.get(subtype)).ifPresent(docs::addAll);
        });
        docs.sort(Comparator.comparing(TickerDocumentDto::getReportPeriod).reversed());
        return convertDocsToJson(docs);
    }

    private String convertDocsToJson(List<TickerDocumentDto> docs) {
        if (docs.isEmpty()) return "[]";
        List<TickerDocumentSlim> slim = docs.stream()
                .map(dto -> {
                    String citationId = citationRegistry.getOrAssignCitationId(TickerDocumentDto.class, dto.getId(), dto);
                    return TickerDocumentSlim.from(dto, citationId, jsonMapper);
                })
                .toList();
        return JsonUtils.toJson(jsonMapper, slim, "[]");
    }

    public String getReverseDcfAnalysis() {
        String key = (company != null ? company.ticker() : "") + "_reverse_dcf";
        return formatter.wrapWithCitation(ReverseDcfDto.class, key, reverseDcfResult);
    }

    public String getValuationScenarios() {
        String key = (company != null ? company.ticker() : "") + "_valuation_scenarios";
        return formatter.wrapWithCitation(ValuationScenarioDto.class, key, valuationScenarios);
    }

    public ValuationScenarioDto getValuationScenariosObject() {
        return valuationScenarios;
    }

    public String getHistoricalValuationPercentiles() {
        return formatter.wrapListWithCitations(
                HistoricalValuationSummaryDto.class,
                historicalValuationPercentiles,
                dto -> (company != null ? company.ticker() : "") + "_" + dto.metric()
        );
    }
}
