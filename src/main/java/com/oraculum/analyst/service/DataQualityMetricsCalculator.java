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

    private static final double DEFAULT_HALF_LIFE_DAYS = 180.0;
    private static final double MARKET_HALF_LIFE_DAYS = 30.0;

    public record DataQualityResult(
            ConfidenceScore confidenceScore,
            List<String> confidenceReasons
    ) {}

    public DataQualityResult calculate(AgentType agentType, AgentContext ctx) {
        LocalDate analysisDate = resolveAnalysisDate(ctx);
        CompanyFactSheetData factSheet = ctx.factSheetData();

        double completeness = evaluateCompleteness(agentType, factSheet);
        long daysAgo = evaluateDataAgeDays(agentType, factSheet, analysisDate);
        double halfLife = getHalfLifeDays(agentType);
        double freshness = calculateFreshness(daysAgo, halfLife);

        double overall = Math.round((0.6 * completeness + 0.4 * freshness) * 100.0) / 100.0;
        double data = Math.round(completeness * 100.0) / 100.0;
        double interp = Math.round(freshness * 100.0) / 100.0;

        ConfidenceScore score = new ConfidenceScore(data, interp, overall);
        List<String> reasons = buildReasons(completeness, freshness, daysAgo);
        return new DataQualityResult(score, reasons);
    }

    private LocalDate resolveAnalysisDate(AgentContext ctx) {
        return ctx.analysisDate() != null ? ctx.analysisDate() : LocalDate.now();
    }

    private double getHalfLifeDays(AgentType agentType) {
        if (agentType == AgentType.SHARE_PRICE || agentType == AgentType.NEWS) {
            return MARKET_HALF_LIFE_DAYS;
        }
        return DEFAULT_HALF_LIFE_DAYS;
    }

    private double calculateFreshness(long daysAgo, double halfLife) {
        if (daysAgo < 0) {
            return 1.0;
        }
        return Math.exp(-daysAgo / halfLife);
    }

    private List<String> buildReasons(double completeness, double freshness, long daysAgo) {
        int compPct = (int) Math.round(completeness * 100.0);
        int freshPct = (int) Math.round(freshness * 100.0);
        return List.of(
                String.format("Data completeness: %d%% based on verified input fields", compPct),
                String.format("Data freshness: %d%% (%d days since latest reporting period)", freshPct, daysAgo)
        );
    }

    private double evaluateCompleteness(AgentType agentType, CompanyFactSheetData data) {
        if (data == null) {
            return 0.0;
        }
        return switch (agentType) {
            case FUNDAMENTALS -> evaluateFundamentalsCompleteness(data);
            case VALUATION -> evaluateValuationCompleteness(data);
            case CASH_FLOW -> evaluateCashFlowCompleteness(data);
            case RISK -> evaluateRiskCompleteness(data);
            case SHARE_PRICE -> evaluateSharePriceCompleteness(data);
            case NEWS -> evaluateNewsCompleteness(data);
            case INSIDER_TRANSACTION -> evaluateInsiderCompleteness(data);
            case SMART_MONEY -> evaluateSmartMoneyCompleteness(data);
            case MACROECONOMIC -> evaluateMacroCompleteness(data);
            case EARNINGS_ESTIMATES -> evaluateEstimatesCompleteness(data);
            default -> 0.8;
        };
    }

    private double evaluateFundamentalsCompleteness(CompanyFactSheetData data) {
        CompanyFinancialRatiosDto latest = getLatestRatio(data);
        if (latest == null) {
            return 0.0;
        }
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
        if (latest == null) {
            return 0.0;
        }
        long nonNulls = Stream.of(latest.freeCashFlow(), latest.fcfMargin(), latest.fcfYoyGrowth())
                .filter(Objects::nonNull).count();
        return nonNulls / 3.0;
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
        int count = data.getDailySharePriceSignalsList().size();
        return Math.min(1.0, count / 20.0);
    }

    private double evaluateNewsCompleteness(CompanyFactSheetData data) {
        int count = data.getRecentNewsList() != null ? data.getRecentNewsList().size() : 0;
        return Math.min(1.0, count / 5.0);
    }

    private double evaluateInsiderCompleteness(CompanyFactSheetData data) {
        if (data.getInsiderTransactionSummaryDto() != null) {
            return 1.0;
        }
        return !data.getRecentInsiderTransactionsList().isEmpty() ? 0.6 : 0.0;
    }

    private double evaluateSmartMoneyCompleteness(CompanyFactSheetData data) {
        return data.getInstitutionalFlowHistory() != null && !data.getInstitutionalFlowHistory().isEmpty() ? 1.0 : 0.0;
    }

    private double evaluateMacroCompleteness(CompanyFactSheetData data) {
        int count = data.getMacroeconomicSummaryList().size();
        return Math.min(1.0, count / 6.0);
    }

    private double evaluateEstimatesCompleteness(CompanyFactSheetData data) {
        return !data.getEarningsEstimatesList().isEmpty() ? 1.0 : 0.0;
    }

    private long evaluateDataAgeDays(AgentType agentType, CompanyFactSheetData data, LocalDate analysisDate) {
        if (data == null) {
            return 180;
        }
        LocalDate latestDate = switch (agentType) {
            case FUNDAMENTALS, VALUATION, CASH_FLOW, RISK -> getLatestRatioDate(data);
            case SHARE_PRICE -> getLatestSharePriceDate(data);
            default -> analysisDate;
        };
        if (latestDate == null) {
            return 180;
        }
        return Math.max(0, ChronoUnit.DAYS.between(latestDate, analysisDate));
    }

    private CompanyFinancialRatiosDto getLatestRatio(CompanyFactSheetData data) {
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
        SharePriceSignalDto signal = data.getLatestDailySignal();
        return signal != null ? signal.tradeDate() : null;
    }
}
