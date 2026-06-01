package com.oraculum.analyst.agents.tools;

import com.oraculum.analyst.domain.StatementVariant;
import com.oraculum.company.api.CompanyApi;
import com.oraculum.company.api.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataToolsImpl implements DataTools {

    private final CompanyApi companyApi;
    private final ObjectMapper objectMapper;

    @Override
    public CompanyDto getCompany(String ticker, String market) {
        return companyApi.getCompany(ticker, market);
    }

    @Override
    public String getIncomeStatementHistory(int companyId, StatementVariant variant, int limit) {
        List<IncomeStatementDto> history = companyApi.getIncomeStatementsByCompanyId(companyId, variant.name(), limit);
        return toMarkdown(history, "Income Statement History for company " + companyId);
    }

    @Override
    public String getBalanceSheetHistory(int companyId, StatementVariant variant, int limit) {
        List<BalanceSheetDto> history = companyApi.getBalanceSheetsByCompanyId(companyId, variant.name(), limit);
        return toMarkdown(history, "Balance Sheet History for company " + companyId);
    }

    @Override
    public String getCashFlowHistory(int companyId, StatementVariant variant, int limit) {
        List<CashFlowStatementDto> history = companyApi.getCashFlowStatementsByCompanyId(companyId, variant.name(), limit);
        return toMarkdown(history, "Cash Flow History for company " + companyId);
    }

    @Override
    public String getSharePriceWindow(int companyId, LocalDate start, LocalDate end) {
        List<SharePriceDto> prices = companyApi.getSharePricesByCompanyId(companyId)
                .stream()
                .filter(p -> !p.tradeDate().isBefore(start) && !p.tradeDate().isAfter(end))
                .collect(Collectors.toList());
        return toMarkdown(prices, "Share Prices for company " + companyId + " from " + start + " to " + end);
    }

    @Override
    public String getSharePriceSignals(int companyId, LocalDate asOf) {
        LocalDate thirtyDaysAgo = asOf.minusDays(30);
        List<DailyMarketSignalDto> dailyResults = companyApi.getDailyMarketSignalsByCompanyId(companyId)
                .stream()
                .filter(s -> !s.tradeDate().isBefore(thirtyDaysAgo) && !s.tradeDate().isAfter(asOf))
                .limit(30)
                .toList();

        LocalDate tenYearsAgo = asOf.minusYears(10);
        List<DailyMarketSignalDto> monthlyResults = companyApi.getDailyMarketSignalsByCompanyId(companyId)
                .stream()
                .filter(s -> !s.tradeDate().isBefore(tenYearsAgo) && !s.tradeDate().isAfter(asOf))
                .filter(s -> s.flagLastDayOfMonth().equals("Y"))
                .limit(120)
                .toList();

        Map<String, List<DailyMarketSignalDto>> data = Map.of("recent_daily",
                dailyResults,
                "historical_monthly",
                monthlyResults);

        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{\"error\": \"Failed to serialize share price signals: " + e.getMessage() + "\"}";
        }
    }

    @Override
    public String getDerivedMetrics(int companyId, StatementVariant variant, int limit) {
        List<DerivedMetricsDto> metrics = companyApi.getDerivedMetricsByCompanyId(companyId)
                .stream()
                .filter(m -> variant.name().equalsIgnoreCase(m.variant()))
                .limit(limit)
                .collect(Collectors.toList());
        return toMarkdown(metrics, "Derived Metrics for company " + companyId);
    }

    @Override
    public String getRecentNews(String ticker, int daysBack, int limit) {
        List<NewsTickerDto> joinedItems = companyApi.getNewsByTicker(ticker, daysBack, limit);
        if (joinedItems == null || joinedItems.isEmpty()) {
            return "No recent news found for this ticker.";
        }

        return joinedItems.stream().map(item -> {
            String sentimentStr = String.format("%s (%.2f)", item.tickerSentimentLabel(), item.tickerSentimentScore());
            return "### " + item.title() + "\n" + "**Date:** " + item.timePublished()
                    .toLocalDate()
                    .toString() + "\n" + "**Source:** " + item.source() + "\n" + "**Ticker Sentiment:** " + sentimentStr + "\n" + "**Summary:** " + item.summary() + "\n";
        }).collect(Collectors.joining("\n---\n"));
    }

    private String toMarkdown(List<?> items, String title) {
        if (items == null || items.isEmpty()) {
            return "No data available.";
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dictItems = items.stream()
                .map(item -> (Map<String, Object>) objectMapper.convertValue(item, Map.class))
                .collect(Collectors.toList());

        List<String> headers = dictItems.getFirst()
                .keySet()
                .stream()
                .filter(h -> !h.equals("template") && !h.equals("variant"))
                .collect(Collectors.toList());

        String headerLine = "| " + String.join(" | ", headers) + " |";
        String separator = "| " + headers.stream().map(_ -> "---").collect(Collectors.joining(" | ")) + " |";

        List<String> rows = dictItems.stream().map(item -> {
            List<String> rowValues = headers.stream()
                    .map(h -> String.valueOf(item.get(h)))
                    .collect(Collectors.toList());
            return "| " + String.join(" | ", rowValues) + " |";
        }).collect(Collectors.toList());

        return "### " + title + "\n" + headerLine + "\n" + separator + "\n" + String.join("\n", rows);
    }
}