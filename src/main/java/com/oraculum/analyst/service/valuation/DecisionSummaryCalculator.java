package com.oraculum.analyst.service.valuation;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.SpecialistOutput;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionDriver;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionSummary;
import com.oraculum.analyst.api.dto.AnalysisResult.ImpliedEpsSnapshot;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.analyst.dto.ValuationScenarioDto;
import com.oraculum.analyst.dto.ValuationScenarioDto.ImpliedEpsEntry;
import com.oraculum.analyst.dto.ValuationScenarioDto.ReverseDcfSummary;
import com.oraculum.analyst.dto.ValuationScenarioDto.SensitivityMatrix;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.company.api.dto.SharePriceSignalDto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class DecisionSummaryCalculator {

    public DecisionSummary buildDecisionSummary(AnalysisResult result, AgentContext ctx) {
        CompanyFactSheetData factSheet = ctx != null ? ctx.factSheetData() : null;
        ValuationScenarioDto scenarios = factSheet != null ? factSheet.getValuationScenariosObject() : null;
        ReverseDcfSummary dcfSummary = scenarios != null ? scenarios.reverseDcfSummary() : null;
        SharePriceSignalDto latestSignal = extractLatestSignal(factSheet);
        CompanyFinancialRatiosDto latestTtm = extractLatestRatio(factSheet, StatementVariant.TTM);

        LocalDate priceDate = latestSignal != null ? latestSignal.tradeDate() : null;
        Integer staleness = calculateStalenessDays(priceDate, ctx != null ? ctx.analysisDate() : null);
        Float currentPrice = resolveCurrentPrice(scenarios, latestSignal);
        Float ttmFcfGrowthPct = (latestTtm != null && latestTtm.fcfYoyGrowth() != null)
                ? round1(latestTtm.fcfYoyGrowth() * 100.0)
                : null;
        SensitivityMatrix matrix = scenarios != null ? scenarios.sensitivityMatrix() : null;

        return DecisionSummary.builder()
                .businessQuality(deriveBusinessQuality(result, ctx))
                .valuationVsExpectations(deriveValuationHurdle(dcfSummary, result))
                .methodAgreement(deriveMethodAgreement(scenarios))
                .marketSetup(deriveMarketSetup(latestSignal, ctx))
                .keyValuationTension(deriveKeyValuationTension(scenarios, dcfSummary))
                .decisiveQuestion(deriveDecisiveQuestion(scenarios, dcfSummary))
                .currentSharePrice(currentPrice)
                .priceDate(priceDate)
                .priceStalenessDays(staleness)
                .nextCatalyst(formatNextCatalyst(scenarios))
                .requiredFcfCagrPct(dcfSummary != null ? dcfSummary.baseCaseImpliedGrowthPct() : null)
                .historicalFcfCagrPct(dcfSummary != null ? dcfSummary.historicalFcfCagrPct() : null)
                .ttmFcfGrowthPct(ttmFcfGrowthPct)
                .targetFiscalYear(matrix != null ? matrix.targetFiscalYear() : null)
                .impliedEpsByMultiple(mapImpliedEpsSnapshots(matrix))
                .build();
    }

    public List<DecisionDriver> buildDecisionDrivers(AgentContext ctx) {
        CompanyFactSheetData factSheet = ctx != null ? ctx.factSheetData() : null;
        if (factSheet == null) {
            return List.of();
        }
        ValuationScenarioDto scenarios = factSheet.getValuationScenariosObject();
        CompanyFinancialRatiosDto ttm = extractLatestRatio(factSheet, StatementVariant.TTM);
        CompanyFinancialRatiosDto qtr = extractLatestRatio(factSheet, StatementVariant.QUARTERLY);

        List<DecisionDriver> drivers = new ArrayList<>();
        addFcfGrowthDriver(drivers, scenarios, ttm);
        addForwardPeDriver(drivers, scenarios);
        addCashConversionDriver(drivers, ttm, qtr);
        addCapitalAllocationDriver(drivers, ttm, qtr);
        return drivers;
    }

    public Integer calculatePriceStalenessDays(AgentContext ctx) {
        if (ctx == null || ctx.factSheetData() == null) {
            return null;
        }
        SharePriceSignalDto latest = extractLatestSignal(ctx.factSheetData());
        LocalDate priceDate = latest != null ? latest.tradeDate() : null;
        return calculateStalenessDays(priceDate, ctx.analysisDate());
    }

    private String deriveBusinessQuality(AnalysisResult result, AgentContext ctx) {
        Double fundScore = resolveAgentScore(result, ctx, "fundamental_health", AgentType.FUNDAMENTALS);
        Double cfScore = resolveAgentScore(result, ctx, null, AgentType.CASH_FLOW);
        double avg = averageNonNull(fundScore, cfScore, 6.0);
        if (avg >= 8.0) return "EXCEPTIONAL";
        if (avg >= 6.8) return "STRONG";
        if (avg >= 5.0) return "MODERATE";
        return "WEAK";
    }

    private String deriveValuationHurdle(ReverseDcfSummary dcfSummary, AnalysisResult result) {
        if (dcfSummary != null && dcfSummary.baseCaseValuationZone() != null) {
            String zone = dcfSummary.baseCaseValuationZone();
            Integer aggPct = dcfSummary.aggressiveOrSpeculativeCellsPct();
            if ("SPECULATIVE".equals(zone) || (aggPct != null && aggPct >= 85)) {
                return "SPECULATIVE";
            }
            if ("AGGRESSIVE".equals(zone) || (aggPct != null && aggPct >= 50)) {
                return "DEMANDING";
            }
            if ("DEEP_DISCOUNT".equals(zone) || "CONSERVATIVE".equals(zone)) {
                return "LOW_HURDLE";
            }
            return "REASONABLE";
        }
        return fallbackValuationHurdle(result);
    }

    private String fallbackValuationHurdle(AnalysisResult result) {
        if (result == null || result.valuation() == null) return "REASONABLE";
        return switch (result.valuation()) {
            case UNDERVALUED -> "LOW_HURDLE";
            case OVERVALUED -> "DEMANDING";
            default -> "REASONABLE";
        };
    }

    private String deriveMethodAgreement(ValuationScenarioDto scenarios) {
        if (scenarios == null) {
            return "ALIGNED";
        }
        if (Boolean.TRUE.equals(scenarios.flagReverseDcfPeDivergence())) {
            return "CONFLICTING_PEAK_MARGIN_RISK";
        }
        if (Boolean.TRUE.equals(scenarios.flagEpsReconciliationDivergence())) {
            return "DIVERGENT";
        }
        return "ALIGNED";
    }

    private String deriveMarketSetup(SharePriceSignalDto signal, AgentContext ctx) {
        if (signal == null || signal.sharePrice() == null) {
            return "WAIT_FOR_CONFIRMATION";
        }
        float price = signal.sharePrice();
        boolean above50 = signal.ma50() != null && price >= signal.ma50();
        boolean above200 = signal.ma200() != null && price >= signal.ma200();
        boolean highDistDays = signal.downDaysHighVol5d() != null && signal.downDaysHighVol5d() >= 3;

        if (highDistDays) {
            return "DISTRIBUTION_WARNING";
        }
        if (above50 && above200) {
            return "SUPPORTIVE_UPTREND";
        }
        if (!above50 && above200) {
            return "PULLBACK_IN_UPTREND";
        }
        Double spScore = resolveAgentScore(null, ctx, null, AgentType.SHARE_PRICE);
        if (signal.ma200() != null && price < signal.ma200() && spScore != null && spScore < 4.5) {
            return "DOWNTREND";
        }
        return "WAIT_FOR_CONFIRMATION";
    }

    private String deriveKeyValuationTension(ValuationScenarioDto scenarios, ReverseDcfSummary dcfSummary) {
        if (scenarios != null && scenarios.valuationMethodTensionNote() != null) {
            return scenarios.valuationMethodTensionNote();
        }
        if (dcfSummary != null && dcfSummary.baseCaseImpliedGrowthPct() != null && scenarios != null && scenarios.forwardPeFy1() != null) {
            return String.format(Locale.ROOT,
                    "FY1 Forward P/E of %.1fx vs. %.1f%% 10Y FCF CAGR required by Reverse DCF (%s zone).",
                    scenarios.forwardPeFy1(),
                    dcfSummary.baseCaseImpliedGrowthPct(),
                    dcfSummary.baseCaseValuationZone() != null ? dcfSummary.baseCaseValuationZone() : "BASE");
        }
        return null;
    }

    private String deriveDecisiveQuestion(ValuationScenarioDto scenarios, ReverseDcfSummary dcfSummary) {
        if (dcfSummary != null && dcfSummary.baseCaseImpliedGrowthPct() != null) {
            return String.format(Locale.ROOT,
                    "Can free cash flow compound at ≥%.1f%% annually over 10 years while margins and working capital normalize?",
                    dcfSummary.baseCaseImpliedGrowthPct());
        }
        if (scenarios != null && scenarios.forwardPeFy1() != null) {
            return String.format(Locale.ROOT,
                    "Can the company deliver on consensus earnings to sustain its %.1fx forward P/E multiple?",
                    scenarios.forwardPeFy1());
        }
        return "Can fundamental cash flow generation sustain current market expectations?";
    }

    private String formatNextCatalyst(ValuationScenarioDto scenarios) {
        if (scenarios == null || scenarios.nextQuarterEndDate() == null) {
            return null;
        }
        if (scenarios.nextQuarterEpsConsensus() != null) {
            return String.format(Locale.ROOT, "Next Quarter End: %s (Consensus EPS $%.2f)",
                    scenarios.nextQuarterEndDate(), scenarios.nextQuarterEpsConsensus());
        }
        return "Next Quarter End: " + scenarios.nextQuarterEndDate();
    }

    private List<ImpliedEpsSnapshot> mapImpliedEpsSnapshots(SensitivityMatrix matrix) {
        if (matrix == null || matrix.impliedEpsByMultiple() == null) {
            return List.of();
        }
        return matrix.impliedEpsByMultiple().stream()
                .map(this::toSnapshot)
                .toList();
    }

    private ImpliedEpsSnapshot toSnapshot(ImpliedEpsEntry e) {
        return new ImpliedEpsSnapshot(
                e.multiple(),
                e.anchorLabel(),
                e.requiredEpsForCurrentPrice(),
                e.consensusEpsAvg(),
                e.consensusVsRequiredPct()
        );
    }

    private void addFcfGrowthDriver(List<DecisionDriver> drivers, ValuationScenarioDto scenarios, CompanyFinancialRatiosDto ttm) {
        ReverseDcfSummary dcf = scenarios != null ? scenarios.reverseDcfSummary() : null;
        if (dcf == null || dcf.baseCaseImpliedGrowthPct() == null) {
            return;
        }
        String current = buildCurrentFcfGrowthText(ttm, dcf);
        String required = String.format(Locale.ROOT, "%.1f%% 10Y FCF CAGR required (%s)",
                dcf.baseCaseImpliedGrowthPct(),
                dcf.baseCaseValuationZone() != null ? dcf.baseCaseValuationZone() : "BASE");
        boolean demanding = "AGGRESSIVE".equals(dcf.baseCaseValuationZone()) || "SPECULATIVE".equals(dcf.baseCaseValuationZone());

        drivers.add(DecisionDriver.builder()
                .factor("Implied FCF Growth Hurdle")
                .status(demanding ? "DEMANDING" : "SUPPORTIVE")
                .importance("CRITICAL")
                .currentValue(current)
                .benchmarkOrRequired(required)
                .whatChangesView(String.format(Locale.ROOT, "FCF YoY growth decelerates below %.1f%% for 2 consecutive quarters", dcf.baseCaseImpliedGrowthPct()))
                .build());
    }

    private String buildCurrentFcfGrowthText(CompanyFinancialRatiosDto ttm, ReverseDcfSummary dcf) {
        if (ttm != null && ttm.fcfYoyGrowth() != null && dcf.historicalFcfCagrPct() != null) {
            return String.format(Locale.ROOT, "%.1f%% TTM YoY (%.1f%% 5Y CAGR)", ttm.fcfYoyGrowth() * 100.0, dcf.historicalFcfCagrPct());
        }
        if (dcf.historicalFcfCagrPct() != null) {
            return String.format(Locale.ROOT, "%.1f%% Historical FCF CAGR", dcf.historicalFcfCagrPct());
        }
        return "See Cash Flow tab";
    }

    private void addForwardPeDriver(List<DecisionDriver> drivers, ValuationScenarioDto scenarios) {
        if (scenarios == null || (scenarios.forwardPeFy1() == null && scenarios.forwardPeFy2() == null)) {
            return;
        }
        String current = String.format(Locale.ROOT, "FY1 P/E %s | FY2 P/E %s",
                scenarios.forwardPeFy1() != null ? String.format(Locale.ROOT, "%.1fx", scenarios.forwardPeFy1()) : "N/A",
                scenarios.forwardPeFy2() != null ? String.format(Locale.ROOT, "%.1fx", scenarios.forwardPeFy2()) : "N/A");
        String required = buildForwardPeBenchmarkText(scenarios.sensitivityMatrix());
        boolean conflict = Boolean.TRUE.equals(scenarios.flagReverseDcfPeDivergence());

        drivers.add(DecisionDriver.builder()
                .factor("Forward Earnings vs. Implied EPS")
                .status(conflict ? "WARNING" : "SUPPORTIVE")
                .importance("CRITICAL")
                .currentValue(current)
                .benchmarkOrRequired(required)
                .whatChangesView("Downward 90-day consensus EPS revisions or management guidance cut")
                .build());
    }

    private String buildForwardPeBenchmarkText(SensitivityMatrix matrix) {
        if (matrix != null && matrix.matrixSummary() != null && matrix.matrixSummary().breakEvenPeOnAvgEps() != null) {
            return String.format(Locale.ROOT, "Break-even P/E %.1fx on consensus EPS $%.2f",
                    matrix.matrixSummary().breakEvenPeOnAvgEps(), matrix.epsAvg());
        }
        return "Sustained positive EPS revision momentum";
    }

    private void addCashConversionDriver(List<DecisionDriver> drivers, CompanyFinancialRatiosDto ttm, CompanyFinancialRatiosDto qtr) {
        if (ttm == null && qtr == null) {
            return;
        }
        Float ttmEq = ttm != null ? ttm.earningsQualityRatio() : null;
        Float qtrEq = qtr != null ? qtr.earningsQualityRatio() : null;
        Float dso = qtr != null && qtr.daysSalesOutstanding() != null ? qtr.daysSalesOutstanding()
                : (ttm != null ? ttm.daysSalesOutstanding() : null);

        String current = String.format(Locale.ROOT, "TTM OCF/NI %s | Qtr OCF/NI %s | DSO %s",
                ttmEq != null ? String.format(Locale.ROOT, "%.2fx", ttmEq) : "N/A",
                qtrEq != null ? String.format(Locale.ROOT, "%.2fx", qtrEq) : "N/A",
                dso != null ? String.format(Locale.ROOT, "%.1f days", dso) : "N/A");
        boolean weakConversion = (ttmEq != null && ttmEq < 0.85f) || (qtrEq != null && qtrEq < 0.70f);

        drivers.add(DecisionDriver.builder()
                .factor("Cash Conversion & Working Capital (DSO)")
                .status(weakConversion ? "WARNING" : "SUPPORTIVE")
                .importance("HIGH")
                .currentValue(current)
                .benchmarkOrRequired("OCF/Net Income ≥ 1.0x and stable DSO")
                .whatChangesView("DSO expands >10% YoY while OCF/NI remains below 0.85x")
                .build());
    }

    private void addCapitalAllocationDriver(List<DecisionDriver> drivers, CompanyFinancialRatiosDto ttm, CompanyFinancialRatiosDto qtr) {
        if (ttm == null && qtr == null) {
            return;
        }
        Float ttmPayout = ttm != null ? ttm.shareholderPayoutToFcfRatioPct() : null;
        Float qtrPayout = qtr != null ? qtr.shareholderPayoutToFcfRatioPct() : null;
        boolean debtFunded = isFlagSet(ttm, qtr, true);
        boolean exceedsFcf = isFlagSet(ttm, qtr, false);

        String current = String.format(Locale.ROOT, "TTM Payout/FCF %s | Latest Qtr %s",
                ttmPayout != null ? String.format(Locale.ROOT, "%.1f%%", ttmPayout) : "N/A",
                qtrPayout != null ? String.format(Locale.ROOT, "%.1f%%", qtrPayout) : "N/A");
        String status = debtFunded ? "RISK" : (exceedsFcf ? "WARNING" : "SUPPORTIVE");

        drivers.add(DecisionDriver.builder()
                .factor("Shareholder Payout & Debt Coverage")
                .status(status)
                .importance("MEDIUM")
                .currentValue(current)
                .benchmarkOrRequired("Payout ≤ 80% of organic FCF without debt funding")
                .whatChangesView("Sustained TTM shareholder payouts >100% of FCF funded by debt issuance")
                .build());
    }

    private boolean isFlagSet(CompanyFinancialRatiosDto ttm, CompanyFinancialRatiosDto qtr, boolean checkDebtFlag) {
        Integer ttmVal = ttm != null ? (checkDebtFlag ? ttm.flagDebtFundedPayout() : ttm.flagPayoutExceedsFcf()) : null;
        Integer qtrVal = qtr != null ? (checkDebtFlag ? qtr.flagDebtFundedPayout() : qtr.flagPayoutExceedsFcf()) : null;
        return Integer.valueOf(1).equals(ttmVal) || Integer.valueOf(1).equals(qtrVal);
    }

    private SharePriceSignalDto extractLatestSignal(CompanyFactSheetData factSheet) {
        if (factSheet == null) return null;
        List<SharePriceSignalDto> signals = factSheet.getDailySharePriceSignalsList();
        return (signals != null && !signals.isEmpty()) ? signals.getFirst() : null;
    }

    private CompanyFinancialRatiosDto extractLatestRatio(CompanyFactSheetData factSheet, StatementVariant variant) {
        if (factSheet == null) return null;
        List<CompanyFinancialRatiosDto> list = factSheet.getCompanyFinancialRatiosList(variant);
        return (list != null && !list.isEmpty()) ? list.getFirst() : null;
    }

    private Float resolveCurrentPrice(ValuationScenarioDto scenarios, SharePriceSignalDto signal) {
        if (scenarios != null && scenarios.currentSharePrice() != null) {
            return scenarios.currentSharePrice();
        }
        return signal != null ? signal.sharePrice() : null;
    }

    private Integer calculateStalenessDays(LocalDate priceDate, LocalDate analysisDate) {
        if (priceDate == null || analysisDate == null) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(priceDate, analysisDate);
        return (int) Math.max(0, days);
    }

    private Double resolveAgentScore(AnalysisResult result, AgentContext ctx, String factorKey, AgentType type) {
        if (ctx != null && ctx.state() != null) {
            Object output = ctx.state().getAgentOutput(type);
            if (output instanceof SpecialistOutput spec) {
                return spec.score();
            }
        }
        if (result != null && result.factorScores() != null && factorKey != null) {
            return result.factorScores().get(factorKey);
        }
        return null;
    }

    private double averageNonNull(Double a, Double b, double fallback) {
        if (a != null && b != null) return (a + b) / 2.0;
        if (a != null) return a;
        if (b != null) return b;
        return fallback;
    }

    private static Float round1(double val) {
        return (float) (Math.round(val * 10.0) / 10.0);
    }
}
