package com.oraculum.analyst.dto;

import com.oraculum.analyst.util.MarkdownUtils;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class CompanyFactSheetData {
    private final ObjectMapper objectMapper;
    private final CompanyDto company;
    private final Map<StatementVariant, List<IncomeStatementDto>> incomeStatements;
    private final Map<StatementVariant, List<BalanceSheetDto>> balanceSheets;
    private final Map<StatementVariant, List<CashFlowStatementDto>> cashFlowStatements;
    private final Map<StatementVariant, List<DerivedMetricsDto>> derivedMetrics;
    private final List<SharePriceDto> sharePriceSignals;
    private final List<DailyMarketSignalDto> dailyMarketSignals;
    private final List<NewsTickerDto> recentNews;

    // Lazy loaded stuff
    private final Map<StatementVariant, String> incomeStatementsCache = new HashMap<>();
    private final Map<StatementVariant, String> balanceSheetsCache = new HashMap<>();
    private final Map<StatementVariant, String> cashFlowStatementsCache = new HashMap<>();
    private final Map<StatementVariant, String> derivedMetricsCache = new HashMap<>();
    private final Map<String, String> sharePriceWindowCache = new HashMap<>(); // Cache for share price window
    private String companyProfileCache;
    private String sharePriceSignalsCache;
    private String dailyMarketSignalsCache;
    private String recentNewsCache;

    public String getCompanyProfile() {
        if (companyProfileCache == null) {
            List<Map<String, Object>> profileData = new ArrayList<>();

            addProfileEntry(profileData, "ticker", company.ticker());
            addProfileEntry(profileData, "market", company.market());
            addProfileEntry(profileData, "company_name", company.companyName(), "Unknown");
            addProfileEntry(profileData, "industry_name", company.industryName(), "Unknown");
            addProfileEntry(profileData, "sector_name", company.sectorName(), "Unknown");
            addProfileEntry(profileData, "isin", company.isin());
            addProfileEntry(profileData, "description", company.description());
            addProfileEntry(profileData, "employee_count", company.employeeCount());
            addProfileEntry(profileData, "currency", company.currency());
            addProfileEntry(profileData, "cik", company.cik());

            companyProfileCache = MarkdownUtils.toMarkdownTable(profileData, "Company Profile", objectMapper);
        }
        return companyProfileCache;
    }

    private void addProfileEntry(List<Map<String, Object>> profileData, String key, Object value) {
        addProfileEntry(profileData, key, value, "");
    }

    private void addProfileEntry(List<Map<String, Object>> profileData, String key, Object value, String defaultValue) {
        String stringValue = (value != null) ? String.valueOf(value) : defaultValue;
        if (!stringValue.isEmpty()) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("Field", capitalizeKey(key));
            entry.put("Value", stringValue);
            profileData.add(entry);
        }
    }

    public String getIncomeStatementHistory(StatementVariant variant) {
        return incomeStatementsCache.computeIfAbsent(variant,
                v -> MarkdownUtils.toMarkdownTable(incomeStatements.get(v),
                        "Income Statement History (" + v.name() + ")",
                        objectMapper));
    }

    public String getBalanceSheetHistory(StatementVariant variant) {
        return balanceSheetsCache.computeIfAbsent(variant,
                v -> MarkdownUtils.toMarkdownTable(balanceSheets.get(v),
                        "Balance Sheet History (" + v.name() + ")",
                        objectMapper));
    }

    public String getCashFlowHistory(StatementVariant variant) {
        return cashFlowStatementsCache.computeIfAbsent(variant,
                v -> MarkdownUtils.toMarkdownTable(cashFlowStatements.get(v),
                        "Cash Flow History (" + v.name() + ")",
                        objectMapper));
    }

    public String getDerivedMetrics(StatementVariant variant) {
        return derivedMetricsCache.computeIfAbsent(variant,
                v -> MarkdownUtils.toMarkdownTable(derivedMetrics.get(v),
                        "Derived Metrics (" + v.name() + ")",
                        objectMapper));
    }

    public String getSharePriceSignals() {
        if (sharePriceSignalsCache == null) {
            if (dailyMarketSignals == null || dailyMarketSignals.isEmpty()) {
                sharePriceSignalsCache = "No daily market signals available.";
                return sharePriceSignalsCache;
            }

            // Assuming sharePriceSignals contains the monthly data, or we fetch it similarly
            // For now, let's use the existing dailyMarketSignals for both, but filter for monthly
            List<DailyMarketSignalDto> dailyResults = dailyMarketSignals.stream().limit(30).toList();

            List<DailyMarketSignalDto> monthlyResults = dailyMarketSignals.stream()
                    .filter(s -> "Y".equalsIgnoreCase(s.flagLastDayOfMonth()))
                    .limit(120)
                    .toList();

            String dailyMarkdown = MarkdownUtils.toMarkdownTable(dailyResults,
                    "Recent Daily Market Signals for company " + company.companyName() + " (last 30 days)",
                    objectMapper);
            String monthlyMarkdown = MarkdownUtils.toMarkdownTable(monthlyResults,
                    "Historical Monthly Market Signals for company " + company.companyName() + " (last 10 years)",
                    objectMapper);

            sharePriceSignalsCache = dailyMarkdown + "\n\n" + monthlyMarkdown;
        }
        return sharePriceSignalsCache;
    }

    public String getRecentNews() {
        if (recentNewsCache == null) {
            if (recentNews == null || recentNews.isEmpty()) {
                recentNewsCache = "No recent news found.";
                return recentNewsCache;
            }

            recentNewsCache = recentNews.stream().map(item -> {
                String sentimentStr = String.format("%s (%.2f)",
                        item.tickerSentimentLabel(),
                        item.tickerSentimentScore());
                return "### " + item.title() + "\n" + "**Date:** " + item.timePublished()
                        .toLocalDate()
                        .toString() + "\n" + "**Source:** " + item.source() + "\n" + "**Ticker Sentiment:** " + sentimentStr + "\n" + "**Summary:** " + item.summary() + "\n";
            }).collect(Collectors.joining("\n---\n"));
        }
        return recentNewsCache;
    }

    private String capitalizeKey(String key) {
        return java.util.Arrays.stream(key.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}
