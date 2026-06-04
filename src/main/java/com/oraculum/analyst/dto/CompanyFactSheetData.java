package com.oraculum.analyst.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.oraculum.analyst.util.JsonUtils;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Builder
@AllArgsConstructor
@Getter
public class CompanyFactSheetData {
    private final ObjectMapper objectMapper;
    private final CompanyDto company;
    private final Map<StatementVariant, List<IncomeStatementDto>> incomeStatements;
    private final Map<StatementVariant, List<BalanceSheetDto>> balanceSheets;
    private final Map<StatementVariant, List<CashFlowStatementDto>> cashFlowStatements;
    private final Map<StatementVariant, List<DerivedMetricsDto>> derivedMetrics;
    private final List<DailyMarketSignalDto> dailyMarketSignals;
    private final List<DailyMarketSignalDto> monthlyMarketSignals;
    private final List<NewsTickerDto> recentNews;
    private final Map<StatementVariant, String> derivedMetricsCache = new HashMap<>();
    // Lazy loaded stuff
    private String companyProfileCache;
    private String dailyMarketSignalsCache;
    private String monthlyMarketSignalsCache;
    private String recentNewsCache;

    public String getCompanyProfile() {
        if (companyProfileCache == null) {
            companyProfileCache = JsonUtils.toJson(objectMapper, company, "{}");
        }
        return companyProfileCache;
    }

    public String getIncomeStatementHistory(StatementVariant variant) {
        List<IncomeStatementDto> stmts = incomeStatements.get(variant);
        if (stmts == null || stmts.isEmpty()) return "[]";
        return "[" + stmts.stream()
                .map(IncomeStatementDto::statementData)
                .collect(Collectors.joining(",")) + "]";
    }

    public String getBalanceSheetHistory(StatementVariant variant) {
        List<BalanceSheetDto> stmts = balanceSheets.get(variant);
        if (stmts == null || stmts.isEmpty()) return "[]";
        return "[" + stmts.stream()
                .map(BalanceSheetDto::statementData)
                .collect(Collectors.joining(",")) + "]";
    }

    public String getCashFlowHistory(StatementVariant variant) {
        List<CashFlowStatementDto> stmts = cashFlowStatements.get(variant);
        if (stmts == null || stmts.isEmpty()) return "[]";
        return "[" + stmts.stream()
                .map(CashFlowStatementDto::statementData)
                .collect(Collectors.joining(",")) + "]";
    }

    public String getDerivedMetrics(StatementVariant variant) {
        return derivedMetricsCache.computeIfAbsent(variant,
                v -> JsonUtils.toJson(objectMapper, derivedMetrics.get(v), "[]"));
    }

    public String getDailyMarketSignals() {
        if (dailyMarketSignalsCache == null) {
            dailyMarketSignalsCache = JsonUtils.toJson(objectMapper, dailyMarketSignals, "[]");
        }
        return dailyMarketSignalsCache;
    }

    public String getMonthlyMarketSignals() {
        if (monthlyMarketSignalsCache == null) {
            monthlyMarketSignalsCache = JsonUtils.toJson(objectMapper, monthlyMarketSignals, "[]");
        }
        return monthlyMarketSignalsCache;
    }

    public String getRecentNews() {
        if (recentNewsCache == null) {
            recentNewsCache = JsonUtils.toJson(objectMapper, recentNews, "[]");
        }
        return recentNewsCache;
    }
}