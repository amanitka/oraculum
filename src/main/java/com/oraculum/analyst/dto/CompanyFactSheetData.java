package com.oraculum.analyst.dto;

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
    private final Map<StatementVariant, List<CompanyFinancialRatiosDto>> companyFinancialRatios;
    private final List<SharePriceSignalDto> dailySharePriceSignals;
    private final List<SharePriceSignalDto> monthlySharePriceSignals;
    private final List<NewsTickerDto> recentNews;
    // Lazy loaded stuff
    private String companyProfileCache;
    @Builder.Default
    private Map<StatementVariant, String> companyFinancialRatiosCache = new HashMap<>();
    private String dailySharePriceSignalsCache;
    private String monthlySharePriceSignalsCache;
    private String recentNewsCache;

    public String getCompanyProfile() {
        if (companyProfileCache == null) {
            companyProfileCache = JsonUtils.toJson(objectMapper, company, "{}");
        }
        return companyProfileCache;
    }

    public String getIncomeStatementHistory(StatementVariant variant) {
        List<IncomeStatementDto> stmts = incomeStatements.get(variant);
        if (stmts == null || stmts.isEmpty())
            return "[]";
        return "[" + stmts.stream().map(IncomeStatementDto::statementData).collect(Collectors.joining(",")) + "]";
    }

    public String getBalanceSheetHistory(StatementVariant variant) {
        List<BalanceSheetDto> stmts = balanceSheets.get(variant);
        if (stmts == null || stmts.isEmpty())
            return "[]";
        return "[" + stmts.stream().map(BalanceSheetDto::statementData).collect(Collectors.joining(",")) + "]";
    }

    public String getCashFlowHistory(StatementVariant variant) {
        List<CashFlowStatementDto> stmts = cashFlowStatements.get(variant);
        if (stmts == null || stmts.isEmpty())
            return "[]";
        return "[" + stmts.stream().map(CashFlowStatementDto::statementData).collect(Collectors.joining(",")) + "]";
    }

    public String getCompanyFinancialRatios(StatementVariant variant) {
        return companyFinancialRatiosCache.computeIfAbsent(variant,
                _ -> JsonUtils.toJson(objectMapper, companyFinancialRatios.get(variant), "[]"));
    }

    public String getDailySharePriceSignals() {
        if (dailySharePriceSignalsCache == null) {
            dailySharePriceSignalsCache = JsonUtils.toJson(objectMapper, dailySharePriceSignals, "[]");
        }
        return dailySharePriceSignalsCache;
    }

    public String getMonthlySharePriceSignals() {
        if (monthlySharePriceSignalsCache == null) {
            monthlySharePriceSignalsCache = JsonUtils.toJson(objectMapper, monthlySharePriceSignals, "[]");
        }
        return monthlySharePriceSignalsCache;
    }

    public String getRecentNews() {
        if (recentNewsCache == null) {
            recentNewsCache = JsonUtils.toJson(objectMapper, recentNews, "[]");
        }
        return recentNewsCache;
    }

    public List<NewsTickerDto> getRecentNewsList() {
        return recentNews;
    }
}