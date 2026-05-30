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
    public TickerDto getTicker(String ticker, String market) {
        return companyApi.getTicker(ticker, market);
    }

    @Override
    public String getIncomeStatementHistory(String ticker, StatementVariant variant, int limit) {
        List<IncomeStatementDto> history = companyApi.getIncomeStatementsByTicker(ticker, variant.name(), limit);
        return toMarkdown(history, "Income Statement History for " + ticker);
    }

    @Override
    public String getBalanceSheetHistory(String ticker, StatementVariant variant, int limit) {
        List<BalanceSheetDto> history = companyApi.getBalanceSheetsByCompanyId(ticker, variant.name(), limit);
        return toMarkdown(history, "Balance Sheet History for " + ticker);
    }

    @Override
    public String getCashFlowHistory(String ticker, StatementVariant variant, int limit) {
        List<CashFlowStatementDto> history = companyApi.getCashFlowStatementsByTicker(ticker, variant.name(), limit);
        return toMarkdown(history, "Cash Flow History for " + ticker);
    }

    @Override
    public String getSharePriceWindow(String ticker, LocalDate start, LocalDate end) {
        List<SharePriceDto> prices = companyApi.getSharePricesByTicker(ticker)
                .stream()
                .filter(p -> !p.tradeDate().isBefore(start) && !p.tradeDate().isAfter(end))
                .collect(Collectors.toList());
        return toMarkdown(prices, "Share Prices for " + ticker + " from " + start + " to " + end);
    }

    @Override
    public String getSharePriceSignals(String ticker, String market, LocalDate asOf) {
        LocalDate thirtyDaysAgo = asOf.minusDays(30);
        List<DailyMarketSignalDto> dailyResults = companyApi.getDailyMarketSignalsByTicker(ticker)
                .stream()
                .filter(s -> market.equals(s.market()))
                .filter(s -> !s.tradeDate().isBefore(thirtyDaysAgo) && !s.tradeDate().isAfter(asOf))
                .limit(30)
                .toList();

        LocalDate tenYearsAgo = asOf.minusYears(10);
        List<DailyMarketSignalDto> monthlyResults = companyApi.getDailyMarketSignalsByTicker(ticker)
                .stream()
                .filter(s -> market.equals(s.market()))
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
    public String getDerivedMetrics(String ticker, StatementVariant variant, int limit) {
        List<DerivedMetricsDto> metrics = companyApi.getDerivedMetricsByTicker(ticker)
                .stream()
                .filter(m -> variant.name().equalsIgnoreCase(m.variant()))
                .limit(limit)
                .collect(Collectors.toList());
        return toMarkdown(metrics, "Derived Metrics for " + ticker);
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

        List<Map<String, Object>> dictItems = items.stream()
                .map(item -> objectMapper.convertValue(item, Map.class))
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