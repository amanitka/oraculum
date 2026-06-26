package com.oraculum.analyst.dto;

import com.oraculum.analyst.util.JsonUtils;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import tools.jackson.databind.JsonNode;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Builder
@AllArgsConstructor
@Getter
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
    private final List<NewsTickerDto> recentNews;
    private final TickerNewsSentimentDto newsSentimentAggregate;
    private final InsiderTransactionSummaryDto insiderTransactionSummary;
    private final List<InsiderTransactionTickerDto> recentInsiderTransactions;

    public String getCompanyProfile() {
        return JsonUtils.toJson(jsonMapper, company, "{}");
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
                    Object payload = getCitationPayload(dto.statementData(), dto);
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
                    Object payload = getCitationPayload(dto.statementData(), dto);
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
                    Object payload = getCitationPayload(dto.statementData(), dto);
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
                    String citationId = citationRegistry.getOrAssignCitationId(CompanyFinancialRatiosDto.class, dto.id(), dto);
                    return CompanyFinancialRatiosSlim.from(dto, citationId);
                })
                .collect(Collectors.toList());
        return JsonUtils.toJson(jsonMapper, slimDtos, "[]");
    }

    public String getDailySharePriceSignals() {
        if (dailySharePriceSignals == null) return "[]";
        List<SharePriceSignalSlim> slim = dailySharePriceSignals.stream()
                .map(SharePriceSignalSlim::from)
                .collect(Collectors.toList());
        return JsonUtils.toJson(jsonMapper, slim, "[]");
    }

    public String getLatestDailySharePriceSignals(int limit) {
        if (dailySharePriceSignals == null || dailySharePriceSignals.isEmpty()) return "[]";
        List<SharePriceSignalSlim> slim = dailySharePriceSignals.stream()
                .limit(limit)
                .map(SharePriceSignalSlim::from)
                .collect(Collectors.toList());
        return JsonUtils.toJson(jsonMapper, slim, "[]");
    }

    public String getMonthlySharePriceSignals() {
        if (monthlySharePriceSignals == null || monthlySharePriceSignals.isEmpty()) {
            return "[]";
        }
        List<SharePriceSignalSlim> slim = monthlySharePriceSignals.stream()
                .map(SharePriceSignalSlim::from)
                .collect(Collectors.toList());
        return JsonUtils.toJson(jsonMapper, slim, "[]");
    }

    public String getRecentNews() {
        if (recentNews == null || recentNews.isEmpty()) return "[]";
        List<NewsTickerSlim> slimNews = recentNews.stream()
                .map(dto -> {
                    String citationId = citationRegistry.getOrAssignCitationId(NewsTickerDto.class, dto.id(), dto);
                    return NewsTickerSlim.from(dto, citationId);
                })
                .collect(Collectors.toList());
        return JsonUtils.toJson(jsonMapper, slimNews, "[]");
    }

    public List<NewsTickerDto> getRecentNewsList() {
        return recentNews;
    }

    public String getNewsSentimentAggregate() {
        return JsonUtils.toJson(jsonMapper, newsSentimentAggregate, "{}");
    }

    public SharePriceSignalDto getLatestDailySignal() {
        if (dailySharePriceSignals != null && !dailySharePriceSignals.isEmpty()) {
            return dailySharePriceSignals.getFirst();
        }
        return null;
    }

    public String getLatestTtmRatios(int periods) {
        List<CompanyFinancialRatiosDto> ttmRatios = companyFinancialRatios.get(StatementVariant.TTM);
        if (ttmRatios == null || ttmRatios.isEmpty()) return "[]";
        List<CompanyFinancialRatiosSlim> slim = ttmRatios.stream()
                .sorted(CompanyFinancialRatiosDto.getComparator().reversed()) // most recent first
                .limit(periods)
                .map(dto -> {
                    String citationId = citationRegistry.getOrAssignCitationId(CompanyFinancialRatiosDto.class, dto.id(), dto);
                    return CompanyFinancialRatiosSlim.from(dto, citationId);
                })
                .collect(Collectors.toList());
        return JsonUtils.toJson(jsonMapper, slim, "[]");
    }

    public String getLatestIndustryRatios(StatementVariant variant) {
        if (industryFinancialRatios == null) return "[]";
        List<IndustryFinancialRatiosDto> ttmRatios = industryFinancialRatios.get(variant);
        if (ttmRatios == null || ttmRatios.isEmpty()) return "[]";
        // Now there is only one row per variant representing the current cross-sectional median
        return JsonUtils.toJson(jsonMapper, industryFinancialRatios.get(variant), "[]");
    }

    public String getInsiderTransactionSummary() {
        return JsonUtils.toJson(jsonMapper, insiderTransactionSummary, "{}");
    }

    public String getRecentInsiderTransactions() {
        if (recentInsiderTransactions == null || recentInsiderTransactions.isEmpty()) {
            return "[]";
        }
        return JsonUtils.toJson(jsonMapper, recentInsiderTransactions, "[]");
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
            return jsonStr;
        }
    }

    private Object getCitationPayload(String jsonStr, Object fallbackDto) {
        if (jsonStr == null || jsonStr.isBlank()) return fallbackDto;
        try {
            return jsonMapper.readTree(jsonStr);
        } catch (Exception e) {
            return fallbackDto;
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
        for (Map.Entry<String, JsonNode> field : source.properties()) {
            String key = field.getKey();

            if (METADATA_KEYS.contains(key)) {
                target.set(key, field.getValue());
            } else if (!isVendorDerivedRatio(key)) {
                target.set(formatKey(key, suffix), field.getValue());
            }
        }
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
}
