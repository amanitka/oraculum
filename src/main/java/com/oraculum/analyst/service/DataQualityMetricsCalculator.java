package com.oraculum.analyst.service;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.StandardAgentOutput.ConfidenceScore;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.company.api.dto.SharePriceSignalDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@Slf4j
public class DataQualityMetricsCalculator {

    private static final long QUARTERLY_BUFFER_DAYS = 120L;
    private static final long MARKET_BUFFER_DAYS = 3L;

    public record DataQualityResult(
            ConfidenceScore confidenceScore,
            List<String> confidenceReasons
    ) {}

    public DataQualityResult calculate(AgentType agentType, AgentContext ctx) {
        LocalDate analysisDate = resolveAnalysisDate(ctx);
        CompanyFactSheetData factSheet = ctx.factSheetData();

        return switch (agentType) {
            case NEWS -> calculateNewsQuality(factSheet);
            case MACROECONOMIC -> calculateMacroQuality(factSheet);
            case SHARE_PRICE -> calculateSharePriceQuality(factSheet, analysisDate);
            case FUNDAMENTALS, VALUATION, CASH_FLOW, RISK -> calculateQuarterlyQuality(agentType, factSheet, analysisDate);
            default -> calculateStandardQuality(agentType, factSheet);
        };
    }

    private LocalDate resolveAnalysisDate(AgentContext ctx) {
        return ctx.analysisDate() != null ? ctx.analysisDate() : LocalDate.now();
    }

    private DataQualityResult calculateNewsQuality(CompanyFactSheetData data) {
        boolean hasNews = data != null && data.getRecentNewsList() != null && !data.getRecentNewsList().isEmpty();
        double score = hasNews ? 1.0 : 0.0;
        List<String> reasons = hasNews
                ? List.of("Data completeness: 100% based on verified input fields", "Data freshness: 100% (time-decayed sentiment active)")
                : List.of("Data completeness: 0% (no news articles found for ticker)", "Data freshness: 0% (no news data)");
        return buildResult(score, reasons);
    }

    private DataQualityResult calculateMacroQuality(CompanyFactSheetData data) {
        boolean hasMacro = data != null && data.getMacroeconomicSummaryList() != null && !data.getMacroeconomicSummaryList().isEmpty();
        double score = hasMacro ? 1.0 : 0.0;
        List<String> reasons = hasMacro
                ? List.of("Data completeness: 100% based on verified input fields", "Data freshness: 100% (current economic observation series)")
                : List.of("Data completeness: 0% (no macroeconomic summary data)", "Data freshness: 0% (no macroeconomic data)");
        return buildResult(score, reasons);
    }

    private DataQualityResult calculateSharePriceQuality(CompanyFactSheetData data, LocalDate analysisDate) {
        double completeness = evaluateSharePriceCompleteness(data);
        LocalDate latest = getLatestSharePriceDate(data);
        long daysAgo = latest != null ? Math.max(0, ChronoUnit.DAYS.between(latest, analysisDate)) : 30;

        double freshness = calculateBufferFreshness(daysAgo, MARKET_BUFFER_DAYS, 14.0);
        String freshReason = String.format("Data freshness: %d%% (%d days since latest market close)",
                (int) Math.round(freshness * 100.0), daysAgo);
        String compReason = String.format("Data completeness: %d%% based on verified input fields",
                (int) Math.round(completeness * 100.0));

        double score = Math.round(completeness * freshness * 100.0) / 100.0;
        return buildResult(score, List.of(compReason, freshReason));
    }

    private DataQualityResult calculateQuarterlyQuality(AgentType agentType, CompanyFactSheetData data, LocalDate analysisDate) {
        double completeness = evaluateCompleteness(agentType, data);
        LocalDate latest = getLatestRatioDate(data);
        long daysAgo = latest != null ? Math.max(0, ChronoUnit.DAYS.between(latest, analysisDate)) : 180;

        double freshness = calculateBufferFreshness(daysAgo, QUARTERLY_BUFFER_DAYS, 90.0);
        String freshReason = daysAgo <= QUARTERLY_BUFFER_DAYS
                ? String.format("Data freshness: 100%% (%d days since quarter end, current filing)", daysAgo)
                : String.format("Data freshness: %d%% (%d days since quarter end, delayed filing)",
                (int) Math.round(freshness * 100.0), daysAgo);
        String compReason = String.format("Data completeness: %d%% based on verified input fields",
                (int) Math.round(completeness * 100.0));

        double score = Math.round(completeness * freshness * 100.0) / 100.0;
        return buildResult(score, List.of(compReason, freshReason));
    }

    private DataQualityResult calculateStandardQuality(AgentType agentType, CompanyFactSheetData data) {
        double completeness = evaluateCompleteness(agentType, data);
        String compReason = String.format("Data completeness: %d%% based on verified input fields",
                (int) Math.round(completeness * 100.0));
        String freshReason = "Data freshness: 100% (current reporting period)";
        double score = Math.round(completeness * 100.0) / 100.0;
        return buildResult(score, List.of(compReason, freshReason));
    }

    private double calculateBufferFreshness(long daysAgo, long bufferDays, double decayDays) {
        if (daysAgo <= bufferDays) {
            return 1.0;
        }
        return Math.exp(-(daysAgo - bufferDays) / decayDays);
    }

    private DataQualityResult buildResult(double score, List<String> reasons) {
        ConfidenceScore conf = new ConfidenceScore(score, 1.0, score);
        return new DataQualityResult(conf, reasons);
    }

    private double evaluateCompleteness(AgentType agentType, CompanyFactSheetData data) {
        if (data == null) return 0.0;
        return switch (agentType) {
            case FUNDAMENTALS -> evaluateFundamentalsCompleteness(data);
            case VALUATION -> evaluateValuationCompleteness(data);
            case CASH_FLOW -> evaluateCashFlowCompleteness(data);
            case RISK -> evaluateRiskCompleteness(data);
            case SHARE_PRICE -> evaluateSharePriceCompleteness(data);
            case INSIDER_TRANSACTION -> evaluateInsiderCompleteness(data);
            case SMART_MONEY -> evaluateSmartMoneyCompleteness(data);
            case EARNINGS_ESTIMATES -> evaluateEstimatesCompleteness(data);
            default -> 1.0;
        };
    }

    private double evaluateFundamentalsCompleteness(CompanyFactSheetData data) {
        CompanyFinancialRatiosDto latest = getLatestRatio(data);
        if (latest == null) return 0.0;
        long nonNulls = Stream.of(
                latest.revenue(), latest.netIncome(), latest.grossMargin(),
                latest.operatingMargin(), latest.returnOnEquity(), latest.epsYoyGrowth(),
                latest.currentRatio(), latest.debtToEquity(), latest.ebitda(), latest.freeCashFlow()
        ).filter(Objects::nonNull).count();
        return nonNulls / 10.0;
    }

    private double evaluateValuationCompleteness(CompanyFactSheetData data) {
        ReverseDcfDto dcf = data.getReverseDcfResult();
        double score = 0.0;
        if (dcf != null) {
            score += 0.4;
            if (dcf.sensitivityMatrix() != null && !dcf.sensitivityMatrix().cells().isEmpty()) {
                score += 0.3;
            }
        }
        if (data.getHistoricalValuationPercentiles() != null && !data.getHistoricalValuationPercentiles().isEmpty()) {
            score += 0.3;
        }
        return score;
    }

    private double evaluateCashFlowCompleteness(CompanyFactSheetData data) {
        CompanyFinancialRatiosDto latest = getLatestRatio(data);
        if (latest == null) return 0.0;
        long nonNulls = Stream.of(
                latest.freeCashFlow(), latest.fcfMargin(), latest.fcfYoyGrowth(),
                latest.daysSalesOutstanding(), latest.daysInventoryOutstanding()
        ).filter(Objects::nonNull).count();
        return nonNulls / 5.0;
    }

    private double evaluateRiskCompleteness(CompanyFactSheetData data) {
        CompanyFinancialRatiosDto latest = getLatestRatio(data);
        double score = latest != null ? 0.6 : 0.0;
        if (data.getRecentSecDocuments() != null && !data.getRecentSecDocuments().isEmpty()) {
            score += 0.4;
        }
        return score;
    }

    private double evaluateSharePriceCompleteness(CompanyFactSheetData data) {
        if (data == null || data.getDailySharePriceSignalsList() == null) return 0.0;
        int count = data.getDailySharePriceSignalsList().size();
        return Math.min(1.0, count / 20.0);
    }

    private double evaluateInsiderCompleteness(CompanyFactSheetData data) {
        if (data == null) return 0.0;
        if (data.getInsiderTransactionSummaryDto() != null || !data.getInsiderTransactionsByPersonList().isEmpty()) {
            return 1.0;
        }
        return 0.0;
    }

    private double evaluateSmartMoneyCompleteness(CompanyFactSheetData data) {
        return data != null && data.getInstitutionalFlowHistory() != null && !data.getInstitutionalFlowHistory().isEmpty() ? 1.0 : 0.0;
    }

    private double evaluateEstimatesCompleteness(CompanyFactSheetData data) {
        return data != null && !data.getEarningsEstimatesList().isEmpty() ? 1.0 : 0.0;
    }

    private CompanyFinancialRatiosDto getLatestRatio(CompanyFactSheetData data) {
        if (data == null) return null;
        List<CompanyFinancialRatiosDto> ratios = data.getCompanyFinancialRatiosList(StatementVariant.TTM);
        if (ratios.isEmpty()) {
            ratios = data.getCompanyFinancialRatiosList(StatementVariant.ANNUAL);
        }
        return ratios.stream()
                .filter(r -> r.reportDate() != null)
                .max(Comparator.comparing(CompanyFinancialRatiosDto::reportDate))
                .orElse(null);
    }

    private LocalDate getLatestRatioDate(CompanyFactSheetData data) {
        CompanyFinancialRatiosDto latest = getLatestRatio(data);
        return latest != null ? latest.reportDate() : null;
    }

    private LocalDate getLatestSharePriceDate(CompanyFactSheetData data) {
        if (data == null) return null;
        SharePriceSignalDto signal = data.getLatestDailySignal();
        return signal != null ? signal.tradeDate() : null;
    }
}
