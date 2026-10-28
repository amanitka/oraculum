package com.oraculum.analyst.dto;

import com.oraculum.analyst.agent.dto.CanonicalFacts;
import com.oraculum.analyst.util.JsonUtils;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.domain.TickerDocumentSubtype;
import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.company.api.dto.*;
import com.oraculum.economy.api.dto.MacroSummaryDto;
import com.oraculum.harvester.api.dto.CompanyNewsDto;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Builder
@AllArgsConstructor
@Getter
@Slf4j
public class CompanyFactSheetData {
    private static final Set<String> METADATA_KEYS = Set.of(
            "Report Date", "Fiscal Year", "Fiscal Period", "Currency", "Ticker",
            "Publish Date", "Restated Date", "Shares (Basic)", "Shares (Diluted)"
    );
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

    private String wrapWithCitation(Class<?> clazz, Object uniqueId, Object dto) {
        if (dto == null) return "{}";
        String citationId = citationRegistry.getOrAssignCitationId(clazz, uniqueId, dto);
        try {
            JsonNode root = jsonMapper.valueToTree(dto);
            if (!root.isObject()) return JsonUtils.toJson(jsonMapper, dto, "{}");
            ObjectNode newNode = (ObjectNode) root;
            newNode.put("citation_id", citationId);
            return jsonMapper.writeValueAsString(newNode);
        } catch (Exception e) {
            return JsonUtils.toJson(jsonMapper, dto, "{}");
        }
    }

    public String getCompanyProfile() {
        return wrapWithCitation(CompanyDto.class, company.id(), company);
    }

    public String getIncomeStatementHistory(StatementVariant variant) {
        return getIncomeStatementHistory(variant, Integer.MAX_VALUE);
    }

    public String getIncomeStatementHistory(StatementVariant variant, int limit) {
        List<IncomeStatementDto> stmts = incomeStatements.get(variant);
        if (stmts == null || stmts.isEmpty()) {
            return "[]";
        }
        return "[" + stmts.stream()
                .limit(limit)
                .map(dto -> {
                    Object payload = getCitationPayload(dto.statementData(), dto, "Income Statement", variant);
                    String citationId = citationRegistry.getOrAssignCitationId(IncomeStatementDto.class, dto.id(), payload);
                    return namespaceJsonMetrics(dto.statementData(), variant, citationId);
                })
                .collect(Collectors.joining(",")) + "]";
    }

    public String getBalanceSheetHistory(StatementVariant variant) {
        return getBalanceSheetHistory(variant, Integer.MAX_VALUE);
    }

    public String getBalanceSheetHistory(StatementVariant variant, int limit) {
        List<BalanceSheetDto> stmts = balanceSheets.get(variant);
        if (stmts == null || stmts.isEmpty()) {
            return "[]";
        }
        return "[" + stmts.stream()
                .limit(limit)
                .map(dto -> {
                    Object payload = getCitationPayload(dto.statementData(), dto, "Balance Sheet", variant);
                    String citationId = citationRegistry.getOrAssignCitationId(BalanceSheetDto.class, dto.id(), payload);
                    return namespaceJsonMetrics(dto.statementData(), variant, citationId);
                })
                .collect(Collectors.joining(",")) + "]";
    }

    public String getCashFlowHistory(StatementVariant variant, int limit) {
        List<CashFlowStatementDto> stmts = cashFlowStatements.get(variant);
        if (stmts == null || stmts.isEmpty())
            return "[]";
        return "[" + stmts.stream()
                .limit(limit)
                .map(dto -> {
                    Object payload = getCitationPayload(dto.statementData(), dto, "Cash Flow Statement", variant);
                    String citationId = citationRegistry.getOrAssignCitationId(CashFlowStatementDto.class, dto.id(), payload);
                    return namespaceJsonMetrics(dto.statementData(), variant, citationId);
                })
                .collect(Collectors.joining(",")) + "]";
    }

    public String getCompanyFinancialRatios(StatementVariant variant) {
        return getCompanyFinancialRatios(variant, Integer.MAX_VALUE);
    }

    public String getCompanyFinancialRatios(StatementVariant variant, int limit) {
        List<CompanyFinancialRatiosDto> dtos = companyFinancialRatios.get(variant);
        if (dtos == null || dtos.isEmpty()) {
            return "[]";
        }
        List<CompanyFinancialRatiosSlim> slimDtos = dtos.stream()
                .limit(limit)
                .map(dto -> {
                    Object payload = getCitationPayloadFromDto(dto, "Financial Ratios", variant);
                    String citationId = citationRegistry.getOrAssignCitationId(CompanyFinancialRatiosDto.class, dto.id(), payload);
                    return CompanyFinancialRatiosSlim.from(dto, citationId);
                })
                .collect(Collectors.toList());
        return JsonUtils.toJson(jsonMapper, slimDtos, "[]");
    }

    /**
     * Returns raw DTOs for code-level consumers (e.g. CanonicalFactsService).
     */
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

        if (ttm == null) {
            log.warn("No TTM ratios for {}. Canonical facts incomplete.", company.ticker());
        }

        SharePriceSignalDto latestSignal = getLatestDailySignal();
        LocalDate latestTradeDate = latestSignal != null ? latestSignal.tradeDate() : null;
        CanonicalFacts facts = CanonicalFacts.from(ttm, annual, latestTradeDate);
        return JsonUtils.toJson(jsonMapper, facts, "{}");
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
                .collect(Collectors.toList());
        return JsonUtils.toJson(jsonMapper, slim, "[]");
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

    public String getRecentCompanyNews(int limit) {
        return serializeFilteredNews(true, limit > 0 ? limit : 50, CompanyNewsSlim.RELEVANCE_DIRECT);
    }

    public String getRecentCompanyNews() {
        return getRecentCompanyNews(50);
    }

    public String getRecentMarketNews(int limit) {
        return serializeFilteredNews(false, limit > 0 ? limit : 5, CompanyNewsSlim.RELEVANCE_MARKET_CONTEXT);
    }

    public String getRecentMarketNews() {
        return getRecentMarketNews(5);
    }

    public String getRecentNews() {
        return getRecentCompanyNews();
    }

    private String serializeFilteredNews(boolean directOnly, int limit, String relevance) {
        List<CompanyNewsDto> filtered = filterNews(directOnly, limit);
        if (filtered.isEmpty()) return "[]";
        List<CompanyNewsSlim> slim = filtered.stream()
                .map(dto -> {
                    String citationId = citationRegistry.getOrAssignCitationId(CompanyNewsDto.class, dto.id(), dto);
                    return CompanyNewsSlim.from(dto, citationId, relevance);
                })
                .collect(Collectors.toList());
        return JsonUtils.toJson(jsonMapper, slim, "[]");
    }

    private List<CompanyNewsDto> filterNews(boolean directOnly, int limit) {
        if (recentNews == null || recentNews.isEmpty()) return List.of();
        String ticker = company != null ? company.ticker() : null;
        String compName = company != null ? company.companyName() : null;
        return recentNews.stream()
                .filter(dto -> directOnly == CompanyNewsSlim.isDirectCompanyNews(dto, ticker, compName))
                .limit(limit)
                .toList();
    }

    public List<CompanyNewsDto> getRecentCompanyNewsList(int limit) {
        return filterNews(true, limit > 0 ? limit : 50);
    }

    public List<CompanyNewsDto> getRecentCompanyNewsList() {
        return getRecentCompanyNewsList(50);
    }

    public List<CompanyNewsDto> getRecentMarketNewsList(int limit) {
        return filterNews(false, limit > 0 ? limit : 5);
    }

    public List<CompanyNewsDto> getRecentMarketNewsList() {
        return getRecentMarketNewsList(5);
    }

    public List<CompanyNewsDto> getRecentNewsList() {
        return recentNews != null ? recentNews : List.of();
    }

    public SharePriceSignalDto getLatestDailySignal() {
        if (dailySharePriceSignals != null && !dailySharePriceSignals.isEmpty()) {
            return dailySharePriceSignals.getFirst();
        }
        return null;
    }

    public String getLatestIndustryRatios(StatementVariant variant) {
        if (industryFinancialRatios == null) return "[]";
        List<IndustryFinancialRatiosDto> ttmRatios = industryFinancialRatios.get(variant);
        if (ttmRatios == null || ttmRatios.isEmpty()) return "[]";
        // Now there is only one row per variant representing the current cross-sectional median
        return wrapWithCitation(IndustryFinancialRatiosDto.class, company.industryName() + "_" + variant, industryFinancialRatios.get(variant));
    }

    public String getInsiderTransactionSummary() {
        return wrapWithCitation(InsiderTransactionSummaryDto.class, company.ticker(), insiderTransactionSummary);
    }

    public String getInsiderTransactionsByPerson() {
        if (insiderTransactionsByPerson == null || insiderTransactionsByPerson.isEmpty()) {
            return "[]";
        }
        return "[" + insiderTransactionsByPerson.stream()
                .map(dto -> wrapWithCitation(InsiderTransactionByPersonDto.class, company.ticker() + "_" + dto.insiderName(), dto))
                .collect(Collectors.joining(",")) + "]";
    }

    public List<InsiderTransactionByPersonDto> getInsiderTransactionsByPersonList() {
        return insiderTransactionsByPerson != null ? insiderTransactionsByPerson : List.of();
    }

    public String getFutureEarningsEstimates(LocalDate analysisDate) {
        if (earningsEstimates == null || earningsEstimates.isEmpty()) {
            return "[]";
        }
        return "[" + earningsEstimates.stream()
                .filter(est -> est.date() != null && est.date().isAfter(analysisDate))
                .map(dto -> wrapWithCitation(EarningsEstimateDto.class, buildEarningsEstimateKey(dto), dto))
                .collect(Collectors.joining(",")) + "]";
    }

    private String buildEarningsEstimateKey(EarningsEstimateDto dto) {
        String base = company.ticker() + "_" + dto.date();
        if (dto.horizon() != null && !dto.horizon().isBlank()) {
            return base + "_" + dto.horizon().trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        }
        return base;
    }

    public String getMacroeconomicSummary() {
        if (macroeconomicSummary == null || macroeconomicSummary.isEmpty()) {
            return "[]";
        }
        return "[" + macroeconomicSummary.stream()
                .map(dto -> wrapWithCitation(MacroSummaryDto.class, dto.indicator(), dto))
                .collect(Collectors.joining(",")) + "]";
    }

    public List<SharePriceSignalDto> getDailySharePriceSignalsList() {
        return dailySharePriceSignals != null ? dailySharePriceSignals : List.of();
    }

    public List<MacroSummaryDto> getMacroeconomicSummaryList() {
        return macroeconomicSummary != null ? macroeconomicSummary : List.of();
    }

    public InsiderTransactionSummaryDto getInsiderTransactionSummaryDto() {
        return insiderTransactionSummary;
    }

    public List<EarningsEstimateDto> getEarningsEstimatesList() {
        return earningsEstimates != null ? earningsEstimates : List.of();
    }

    public String getInstitutionalFlowHistoryJson() {
        if (institutionalFlowHistory == null || institutionalFlowHistory.isEmpty()) {
            return "[]";
        }
        return "[" + institutionalFlowHistory.stream()
                .map(dto -> wrapWithCitation(CompanyInstitutionalFlowDto.class, company.ticker() + "_" + dto.reportPeriod(), dto))
                .collect(Collectors.joining(",")) + "]";
    }

    public String getSectorRotationSummaryJson() {
        if (allSectorsInstitutionalFlow == null || allSectorsInstitutionalFlow.isEmpty()) {
            return "[]";
        }
        return "[" + allSectorsInstitutionalFlow.stream()
                .map(dto -> wrapWithCitation(SectorInstitutionalFlowTrendDto.class, dto.sectorName() + "_" + dto.reportPeriod(), dto))
                .collect(Collectors.joining(",")) + "]";
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
        if (docs.isEmpty()) {
            return "[]";
        }

        List<TickerDocumentSlim> slim = docs.stream()
                .map(dto -> {
                    String citationId = citationRegistry.getOrAssignCitationId(TickerDocumentDto.class, dto.getId(), dto);
                    return TickerDocumentSlim.from(dto, citationId, jsonMapper);
                })
                .collect(Collectors.toList());

        return JsonUtils.toJson(jsonMapper, slim, "[]");
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

    private String namespaceJsonMetrics(String jsonStr, StatementVariant variant, String citationId) {
        if (jsonStr == null || jsonStr.isBlank()) return "{}";
        try {
            JsonNode root = jsonMapper.readTree(jsonStr);
            if (!root.isObject()) return jsonStr;

            ObjectNode newNode = jsonMapper.createObjectNode();
            if (citationId != null) {
                newNode.put("citation_id", citationId);
            }
            processJsonFields((ObjectNode) root, newNode, getVariantSuffix(variant));

            return jsonMapper.writeValueAsString(newNode);
        } catch (Exception e) {
            log.warn("Failed to namespace json metrics. Variant: {}, Citation ID: {}", variant, citationId, e);
            return jsonStr;
        }
    }

    private Object getCitationPayload(String jsonStr, Object fallbackDto, String sourceName, StatementVariant variant) {
        if (jsonStr == null || jsonStr.isBlank()) return fallbackDto;
        try {
            JsonNode node = jsonMapper.readTree(jsonStr);
            if (node.isObject()) {
                ObjectNode objNode = (ObjectNode) node;
                if (sourceName != null) {
                    objNode.put("_source", sourceName);
                }
                if (variant != null) {
                    objNode.put("_variant", variant.name());
                }
            }
            return node;
        } catch (Exception e) {
            log.warn("Failed to parse json string for citation payload. Source: {}", sourceName, e);
            return fallbackDto;
        }
    }

    private Object getCitationPayloadFromDto(Object dto, String sourceName, StatementVariant variant) {
        try {
            JsonNode node = jsonMapper.valueToTree(dto);
            if (node.isObject()) {
                ObjectNode objNode = (ObjectNode) node;
                if (sourceName != null) {
                    objNode.put("_source", sourceName);
                }
                if (variant != null) {
                    objNode.put("_variant", variant.name());
                }
                return objNode;
            }
            return dto;
        } catch (Exception e) {
            log.warn("Failed to convert dto to json tree for citation payload. Source: {}", sourceName, e);
            return dto;
        }
    }

    private String getVariantSuffix(StatementVariant variant) {
        return switch (variant) {
            case QUARTERLY -> "_q";
            case TTM -> "_ttm";
            case ANNUAL -> "_a";
        };
    }

    private void processJsonFields(ObjectNode source, ObjectNode target, String suffix) {
        addPeriodEndLabel(source, target);
        for (Map.Entry<String, JsonNode> field : source.properties()) {
            String key = field.getKey();

            if (METADATA_KEYS.contains(key)) {
                target.set(key, field.getValue());
            } else if (!isVendorDerivedRatio(key)) {
                target.set(formatKey(key, suffix), field.getValue());
            }
        }
    }

    private void addPeriodEndLabel(ObjectNode source, ObjectNode target) {
        String reportDate = source.path("Report Date").asString("");
        if (reportDate.isBlank()) {
            return;
        }
        String fiscalPeriod = source.path("Fiscal Period").asString("");
        String label = fiscalPeriod.isBlank()
                ? "ended " + reportDate
                : fiscalPeriod + " (ended " + reportDate + ")";
        target.put("Period End Label", label);
    }

    private boolean isVendorDerivedRatio(String key) {
        String lower = key.toLowerCase();
        return lower.contains("margin") || lower.contains("ratio") ||
                lower.contains("growth") || lower.contains("return") ||
                lower.contains("yield");
    }

    private String formatKey(String key, String suffix) {
        String snakeCase = key.toLowerCase()
                .replaceAll("[\\s\\-()]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return snakeCase + suffix;
    }

    public String getReverseDcfAnalysis() {
        return wrapWithCitation(ReverseDcfDto.class, company.ticker() + "_reverse_dcf", reverseDcfResult);
    }

    public String getValuationScenarios() {
        return wrapWithCitation(ValuationScenarioDto.class, company.ticker() + "_valuation_scenarios", valuationScenarios);
    }

    public ValuationScenarioDto getValuationScenariosObject() {
        return valuationScenarios;
    }

    public String getHistoricalValuationPercentiles() {
        if (historicalValuationPercentiles == null || historicalValuationPercentiles.isEmpty()) {
            return "[]";
        }
        return "[" + historicalValuationPercentiles.stream()
                .map(summary -> wrapWithCitation(HistoricalValuationSummaryDto.class, company.ticker() + "_" + summary.metric(), summary))
                .collect(Collectors.joining(",")) + "]";
    }

}
