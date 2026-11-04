package com.oraculum.analyst.service;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.StandardAgentOutput.ConfidenceScore;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.analyst.dto.CompanyNewsSlim;
import com.oraculum.analyst.dto.quality.DataQualityResult;
import com.oraculum.analyst.dto.quality.InterpretationConfidenceResult;
import com.oraculum.analyst.dto.valuation.ValuationScenarioDto;
import com.oraculum.analyst.service.macro.SectorMacroIndicatorPolicy;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.company.api.dto.CompanyInstitutionalFlowDto;
import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.company.api.dto.SharePriceSignalDto;
import com.oraculum.economy.api.domain.MacroIndicator;
import com.oraculum.harvester.api.dto.CompanyNewsDto;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Service
@Slf4j
public class DataQualityMetricsCalculator {

    private static final long QUARTERLY_BUFFER_DAYS = 120L;
    private static final long MARKET_BUFFER_DAYS = 3L;
    private static final int SEC_EX991_MAX_AGE_DAYS = 75;

    private final SectorMacroIndicatorPolicy sectorMacroIndicatorPolicy;

    @Autowired
    public DataQualityMetricsCalculator(SectorMacroIndicatorPolicy sectorMacroIndicatorPolicy) {
        this.sectorMacroIndicatorPolicy = sectorMacroIndicatorPolicy;
    }

    public DataQualityMetricsCalculator() {
        this(new SectorMacroIndicatorPolicy());
    }

    public DataQualityResult calculate(AgentType agentType, AgentContext ctx) {
        LocalDate analysisDate = resolveAnalysisDate(ctx);
        CompanyFactSheetData factSheet = ctx.factSheetData();

        return switch (agentType) {
            case NEWS -> calculateNewsQuality(agentType, factSheet, analysisDate);
            case MACROECONOMIC -> calculateMacroQuality(agentType, factSheet, analysisDate);
            case SHARE_PRICE -> calculateSharePriceQuality(agentType, factSheet, analysisDate);
            case FUNDAMENTALS, VALUATION, CASH_FLOW, RISK ->
                    calculateQuarterlyQuality(agentType, factSheet, analysisDate);
            default -> calculateStandardQuality(agentType, factSheet, analysisDate);
        };
    }

    private LocalDate resolveAnalysisDate(AgentContext ctx) {
        return ctx.analysisDate() != null ? ctx.analysisDate() : LocalDate.now();
    }

    private DataQualityResult calculateNewsQuality(
            AgentType agentType,
            CompanyFactSheetData data,
            LocalDate analysisDate
    ) {
        boolean hasNews = data != null && !data.getRecentNewsList().isEmpty();
        boolean hasSecRelease = hasRecentSecPressRelease(data, analysisDate);
        double score = (hasNews || hasSecRelease) ? 1.0 : 0.0;
        List<String> reasons = (hasNews || hasSecRelease)
                ? List.of("Data completeness: 100% based on verified input fields", "Data freshness: 100% (recent news or SEC 8-K release available)")
                : List.of("Data completeness: 0% (no news articles found for ticker)", "Data freshness: 0% (no news data)");
        return buildResult(agentType, data, analysisDate, score, reasons);
    }

    private boolean hasRecentSecPressRelease(CompanyFactSheetData data, LocalDate analysisDate) {
        if (data == null) {
            return false;
        }
        String json = data.getRecentSecEx991Summaries(analysisDate, SEC_EX991_MAX_AGE_DAYS);
        return json != null && !json.isBlank() && !"[]".equals(json);
    }

    private DataQualityResult calculateMacroQuality(
            AgentType agentType,
            CompanyFactSheetData data,
            LocalDate analysisDate
    ) {
        boolean hasMacro = hasRelevantMacroData(data);
        double score = hasMacro ? 1.0 : 0.0;
        List<String> reasons = hasMacro
                ? List.of("Data completeness: 100% based on verified input fields", "Data freshness: 100% (current economic observation series)")
                : List.of("Data completeness: 0% (no macroeconomic summary data)", "Data freshness: 0% (no macroeconomic data)");
        return buildResult(agentType, data, analysisDate, score, reasons);
    }

    private boolean hasRelevantMacroData(CompanyFactSheetData data) {
        if (data == null || data.getMacroeconomicSummaryList().isEmpty()) {
            return false;
        }
        String sector = data.getCompany() != null ? data.getCompany().sectorName() : null;
        Set<MacroIndicator> relevant = sectorMacroIndicatorPolicy.getRelevantIndicators(sector);
        return data.getMacroeconomicSummaryList().stream()
                .anyMatch(m -> m != null && m.indicator() != null && relevant.contains(m.indicator()));
    }

    private DataQualityResult calculateSharePriceQuality(
            AgentType agentType,
            CompanyFactSheetData data,
            LocalDate analysisDate
    ) {
        double completeness = evaluateSharePriceCompleteness(data);
        LocalDate latest = getLatestSharePriceDate(data);
        long daysAgo = latest != null ? Math.max(0, ChronoUnit.DAYS.between(latest, analysisDate)) : 30;

        double freshness = calculateBufferFreshness(daysAgo, MARKET_BUFFER_DAYS, 14.0);
        String freshReason = latest != null
                ? String.format(Locale.ROOT, "Data freshness: %d%% (latest close %s, %d days ago)",
                (int) Math.round(freshness * 100.0), latest, daysAgo)
                : String.format(Locale.ROOT, "Data freshness: %d%% (%d days since latest market close)",
                (int) Math.round(freshness * 100.0), daysAgo);
        String compReason = String.format(Locale.ROOT, "Data completeness: %d%% based on verified input fields",
                (int) Math.round(completeness * 100.0));

        double score = Math.round(completeness * freshness * 100.0) / 100.0;
        return buildResult(agentType, data, analysisDate, score, List.of(compReason, freshReason));
    }

    private DataQualityResult calculateQuarterlyQuality(
            AgentType agentType,
            CompanyFactSheetData data,
            LocalDate analysisDate
    ) {
        double completeness = evaluateCompleteness(agentType, data);
        LocalDate latest = getLatestRatioDate(data);
        long daysAgo = latest != null ? Math.max(0, ChronoUnit.DAYS.between(latest, analysisDate)) : 180;

        double freshness = calculateBufferFreshness(daysAgo, QUARTERLY_BUFFER_DAYS, 90.0);
        String freshReason = daysAgo <= QUARTERLY_BUFFER_DAYS
                ? String.format(Locale.ROOT, "Data freshness: 100%% (%d days since quarter end, current filing)", daysAgo)
                : String.format(Locale.ROOT, "Data freshness: %d%% (%d days since quarter end, delayed filing)",
                (int) Math.round(freshness * 100.0), daysAgo);
        String compReason = String.format(Locale.ROOT, "Data completeness: %d%% based on verified input fields",
                (int) Math.round(completeness * 100.0));

        double score = Math.round(completeness * freshness * 100.0) / 100.0;
        return buildResult(agentType, data, analysisDate, score, List.of(compReason, freshReason));
    }

    private DataQualityResult calculateStandardQuality(
            AgentType agentType,
            CompanyFactSheetData data,
            LocalDate analysisDate
    ) {
        double completeness = evaluateCompleteness(agentType, data);
        String compReason = String.format(Locale.ROOT, "Data completeness: %d%% based on verified input fields",
                (int) Math.round(completeness * 100.0));
        String freshReason = "Data freshness: 100% (current reporting period)";
        double score = Math.round(completeness * 100.0) / 100.0;
        return buildResult(agentType, data, analysisDate, score, List.of(compReason, freshReason));
    }

    private double calculateBufferFreshness(long daysAgo, long bufferDays, double decayDays) {
        if (daysAgo <= bufferDays) {
            return 1.0;
        }
        return Math.exp(-(daysAgo - bufferDays) / decayDays);
    }

    private DataQualityResult buildResult(
            AgentType agentType,
            CompanyFactSheetData data,
            LocalDate analysisDate,
            double score,
            List<String> reasons
    ) {
        InterpretationConfidenceResult interp = evaluateInterpretationQuality(agentType, data, analysisDate);
        double overall = Math.round(((score * 0.5) + (interp.score() * 0.5)) * 100.0) / 100.0;
        ConfidenceScore conf = new ConfidenceScore(score, interp.score(), overall);

        List<String> combined = new ArrayList<>(reasons);
        combined.add(String.format(
                Locale.ROOT,
                "Interpretation confidence: %d%% (%s)",
                (int) Math.round(interp.score() * 100.0),
                interp.reason()
        ));
        return new DataQualityResult(conf, combined);
    }

    private InterpretationConfidenceResult evaluateInterpretationQuality(
            AgentType agentType,
            CompanyFactSheetData data,
            LocalDate analysisDate
    ) {
        if (data == null) {
            return new InterpretationConfidenceResult(0.50, "Fact sheet data is unavailable");
        }
        return switch (agentType) {
            case VALUATION -> evaluateValuationInterpretation(data);
            case SMART_MONEY -> evaluateSmartMoneyInterpretation(data);
            case EARNINGS_ESTIMATES -> evaluateEstimatesInterpretation(data);
            case FUNDAMENTALS, CASH_FLOW -> evaluateFundamentalsInterpretation(data);
            case NEWS -> evaluateNewsInterpretation(data, analysisDate);
            default -> new InterpretationConfidenceResult(1.0, "Verified input signals without structural divergences");
        };
    }

    private InterpretationConfidenceResult evaluateValuationInterpretation(CompanyFactSheetData data) {
        ValuationScenarioDto scenarios = data.getValuationScenariosObject();
        if (scenarios == null) {
            return new InterpretationConfidenceResult(0.85, "Standard valuation inputs available");
        }
        double score = 1.0;
        List<String> penalties = new ArrayList<>();

        if (scenarios.twoStageReverseDcf() != null && Boolean.TRUE.equals(scenarios.twoStageReverseDcf().flagFcfProxyDivergence())) {
            score -= 0.20;
            penalties.add("forward FCF proxy divergence");
        }
        if (Boolean.TRUE.equals(scenarios.flagReverseDcfPeDivergence())) {
            score -= 0.15;
            penalties.add("DCF vs P/E tension");
        }
        if ("MATERIAL_DIVERGENCE".equals(scenarios.epsReconciliationStatus())) {
            score -= 0.15;
            penalties.add("quarterly/annual EPS divergence");
        }
        return buildInterpretationResult(score, penalties);
    }

    private InterpretationConfidenceResult evaluateSmartMoneyInterpretation(CompanyFactSheetData data) {
        List<CompanyInstitutionalFlowDto> flows = data.getInstitutionalFlowHistory();
        if (flows == null || flows.isEmpty()) {
            return new InterpretationConfidenceResult(0.80, "No historical institutional flow records");
        }
        double score = 1.0;
        List<String> penalties = new ArrayList<>();
        if (flows.stream().anyMatch(f -> Boolean.TRUE.equals(f.flagIncompleteFilerSample()))) {
            score -= 0.30;
            penalties.add("incomplete 13F filer sample");
        }
        return buildInterpretationResult(score, penalties);
    }

    private InterpretationConfidenceResult evaluateEstimatesInterpretation(CompanyFactSheetData data) {
        List<EarningsEstimateDto> estimates = data.getEarningsEstimatesList();
        if (estimates == null || estimates.isEmpty()) {
            return new InterpretationConfidenceResult(0.70, "No earnings estimates available");
        }
        double score = 1.0;
        List<String> penalties = new ArrayList<>();
        if (estimates.stream().anyMatch(this::hasWideEpsSpread)) {
            score -= 0.20;
            penalties.add("wide consensus EPS dispersion");
        }
        return buildInterpretationResult(score, penalties);
    }

    private boolean hasWideEpsSpread(EarningsEstimateDto estimate) {
        if (estimate.epsEstimateHigh() == null || estimate.epsEstimateLow() == null || estimate.epsEstimateAverage() == null) {
            return false;
        }
        double avg = estimate.epsEstimateAverage().doubleValue();
        if (avg <= 0.1) {
            return false;
        }
        return ((estimate.epsEstimateHigh().doubleValue() - estimate.epsEstimateLow().doubleValue()) / avg) > 0.35;
    }

    private InterpretationConfidenceResult evaluateFundamentalsInterpretation(CompanyFactSheetData data) {
        CompanyFinancialRatiosDto ratio = getLatestRatio(data);
        if (ratio == null) {
            return new InterpretationConfidenceResult(0.75, "No recent fundamental ratio period found");
        }
        double score = 1.0;
        List<String> penalties = new ArrayList<>();
        if (Integer.valueOf(1).equals(ratio.flagHighNonOperatingIncome())) {
            score -= 0.15;
            penalties.add("high non-operating income distortion");
        }
        if (ratio.earningsQualityRatio() != null && ratio.earningsQualityRatio() < 0.70f) {
            score -= 0.15;
            penalties.add("low cash earnings conversion");
        }
        return buildInterpretationResult(score, penalties);
    }

    private InterpretationConfidenceResult evaluateNewsInterpretation(CompanyFactSheetData data, LocalDate analysisDate) {
        List<CompanyNewsDto> news = data.getRecentNewsList();
        boolean hasSecRelease = hasRecentSecPressRelease(data, analysisDate);
        if (news.isEmpty() && !hasSecRelease) {
            return new InterpretationConfidenceResult(0.60, "No news coverage available");
        }
        String ticker = data.getCompany() != null ? data.getCompany().ticker() : null;
        String name = data.getCompany() != null ? data.getCompany().companyName() : null;
        boolean hasDirect = news.stream().anyMatch(n -> CompanyNewsSlim.isDirectCompanyNews(n, ticker, name));
        if (hasDirect) {
            return new InterpretationConfidenceResult(1.0, "Direct company news coverage available");
        }
        if (hasSecRelease) {
            return new InterpretationConfidenceResult(0.95, "Official SEC 8-K press release coverage available");
        }
        return new InterpretationConfidenceResult(0.75, "Market context only, no direct company news");
    }

    private InterpretationConfidenceResult buildInterpretationResult(double score, List<String> penalties) {
        double clamped = Math.round(Math.clamp(score, 0.30, 1.0) * 100.0) / 100.0;
        if (penalties.isEmpty()) {
            return new InterpretationConfidenceResult(clamped, "Verified input signals without structural divergences");
        }
        return new InterpretationConfidenceResult(clamped, "Penalized for: " + String.join(", ", penalties));
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
            case INSIDER_TRANSACTION -> evaluateInsiderCompleteness(data);
            case SMART_MONEY -> evaluateSmartMoneyCompleteness(data);
            case EARNINGS_ESTIMATES -> evaluateEstimatesCompleteness(data);
            default -> 1.0;
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
        if (data == null || data.getDailySharePriceSignalsList() == null) {
            return 0.0;
        }
        return Math.min(1.0, data.getDailySharePriceSignalsList().size() / 20.0);
    }

    private double evaluateInsiderCompleteness(CompanyFactSheetData data) {
        if (data == null) {
            return 0.0;
        }
        return (data.getInsiderTransactionSummaryDto() != null || !data.getInsiderTransactionsByPersonList().isEmpty())
                ? 1.0
                : 0.0;
    }

    private double evaluateSmartMoneyCompleteness(CompanyFactSheetData data) {
        return data != null && data.getInstitutionalFlowHistory() != null && !data.getInstitutionalFlowHistory().isEmpty()
                ? 1.0
                : 0.0;
    }

    private double evaluateEstimatesCompleteness(CompanyFactSheetData data) {
        return data != null && !data.getEarningsEstimatesList().isEmpty() ? 1.0 : 0.0;
    }

    private CompanyFinancialRatiosDto getLatestRatio(CompanyFactSheetData data) {
        if (data == null) {
            return null;
        }
        List<CompanyFinancialRatiosDto> ratios = data.getCompanyFinancialRatiosList(StatementVariant.TTM);
        if (ratios.isEmpty()) {
            ratios = data.getCompanyFinancialRatiosList(StatementVariant.ANNUAL);
        }
        return ratios.stream()
                .filter(r -> r != null && r.reportDate() != null)
                .max(Comparator.comparing(CompanyFinancialRatiosDto::reportDate))
                .orElse(null);
    }

    private LocalDate getLatestRatioDate(CompanyFactSheetData data) {
        CompanyFinancialRatiosDto latest = getLatestRatio(data);
        return latest != null ? latest.reportDate() : null;
    }

    private LocalDate getLatestSharePriceDate(CompanyFactSheetData data) {
        if (data == null) {
            return null;
        }
        SharePriceSignalDto signal = data.getLatestDailySignal();
        return signal != null ? signal.tradeDate() : null;
    }
}
