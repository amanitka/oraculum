package com.oraculum.analyst.service.valuation;

import com.oraculum.analyst.dto.valuation.FcfMarginSensitivityEntryDto;
import com.oraculum.analyst.dto.valuation.ForwardDcfSolverResult;
import com.oraculum.analyst.dto.valuation.ForwardFcfProxyRequest;
import com.oraculum.analyst.dto.valuation.ForwardReverseDcfDto;
import com.oraculum.analyst.dto.valuation.ForwardReverseDcfRequest;
import com.oraculum.analyst.dto.valuation.MultiYearFcfProxyResult;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.company.api.dto.ValuationZone;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import com.oraculum.util.NumericUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class ForwardReverseDcfCalculator {

    private static final double DEFAULT_DISCOUNT_RATE = 0.10d;
    private static final double DEFAULT_TERMINAL_GROWTH = 0.03d;
    private static final int DEFAULT_PROJECTION_YEARS = 10;
    private static final double SOLVER_LOW = -0.50d;
    private static final double SOLVER_HIGH = 0.60d;
    private static final double SOLVER_TOLERANCE = 0.0001d;

    private final ForwardFcfProxyDeriver proxyDeriver;

    public ForwardReverseDcfCalculator() {
        this(new ForwardFcfProxyDeriver());
    }

    public ForwardReverseDcfDto calculate(ForwardReverseDcfRequest request) {
        if (!isValidRequest(request)) {
            return null;
        }
        EarningsEstimateDto fy1 = resolveFy1(request);
        EarningsEstimateDto fy2 = resolveFy2(request);
        if (fy1 == null && fy2 == null) {
            return null;
        }
        MultiYearFcfProxyResult proxy = deriveForwardProxies(request, fy1, fy2);
        Double stage2BaseFcf = resolveStage2BaseFcf(proxy);
        if (stage2BaseFcf == null || stage2BaseFcf <= 0) {
            return null;
        }
        return solveModel(request, fy1, fy2, proxy);
    }

    private Double resolveStage2BaseFcf(MultiYearFcfProxyResult proxy) {
        if (proxy == null) {
            return null;
        }
        return proxy.fy2FcfProxy() != null ? proxy.fy2FcfProxy() : proxy.fy1FcfProxy();
    }

    private boolean isValidRequest(ForwardReverseDcfRequest request) {
        if (request == null || request.marketCap() == null || request.marketCap() <= 0 || request.analysisDate() == null) {
            return false;
        }
        return request.fy1() != null || request.fy2() != null || !request.estimates().isEmpty();
    }

    private MultiYearFcfProxyResult deriveForwardProxies(
            ForwardReverseDcfRequest request,
            EarningsEstimateDto fy1,
            EarningsEstimateDto fy2
    ) {
        Double dilutedShares = resolveDilutedShares(request.ttmRatio(), request.marketCap(), request.currentSharePrice());
        return proxyDeriver.derive(ForwardFcfProxyRequest.builder()
                .fy1(fy1)
                .fy2(fy2)
                .annualRatios(request.annualRatios())
                .ttmRatio(request.ttmRatio())
                .dilutedShares(dilutedShares)
                .build());
    }

    private ForwardReverseDcfDto solveModel(
            ForwardReverseDcfRequest request,
            EarningsEstimateDto fy1,
            EarningsEstimateDto fy2,
            MultiYearFcfProxyResult proxy
    ) {
        double t1 = computeTimeHorizonYears(request.analysisDate(), fy1 != null ? fy1.date() : null, 0.5);
        double t2 = computeTimeHorizonYears(request.analysisDate(), fy2 != null ? fy2.date() : null, t1 + 1.0);
        double fy1Stub = proxy.fy1FcfProxy() != null ? proxy.fy1FcfProxy() * Math.clamp(t1, 0.1, 1.0) : 0.0;
        double stage2BaseFcf = resolveStage2BaseFcf(proxy);

        ForwardDcfSolverResult solver = solveImpliedGrowthRate(request.marketCap(), fy1Stub, stage2BaseFcf, t1, t2);
        Float impliedGrowthPct = NumericUtil.round1(solver.impliedGrowth() * 100.0);
        Float consensusGrowth = computeConsensusCagr(fy2, request.ttmRatio(), t2);
        Float historicalCagr = computeHistoricalFcfCagr(request.annualRatios());
        ValuationZone zone = determineUnifiedValuationZone(impliedGrowthPct);
        List<FcfMarginSensitivityEntryDto> sensitivity = buildMarginSensitivity(
                request, fy1, fy2, fy1Stub, stage2BaseFcf, t1, t2
        );

        return buildResultDto(request.marketCap(), proxy, solver, impliedGrowthPct, zone, sensitivity, consensusGrowth, historicalCagr);
    }

    private ForwardReverseDcfDto buildResultDto(
            Double marketCap,
            MultiYearFcfProxyResult proxy,
            ForwardDcfSolverResult solver,
            Float impliedGrowthPct,
            ValuationZone zone,
            List<FcfMarginSensitivityEntryDto> sensitivity,
            Float consensusGrowth,
            Float historicalCagr
    ) {
        return ForwardReverseDcfDto.builder()
                .marketCapUsd(marketCap)
                .fy1FcfProxyUsd(proxy.fy1FcfProxy())
                .fy2FcfProxyUsd(proxy.fy2FcfProxy())
                .fcfProxyDerivationMethod(proxy.derivationMethod())
                .flagFcfProxyDivergence(proxy.flagDivergence())
                .fcfProxyDivergencePct(proxy.maxDivergencePct())
                .impliedGrowthYears3To10Pct(impliedGrowthPct)
                .relativeValuationZone(zone)
                .marginSensitivity(sensitivity)
                .consensusGrowthRatePct(consensusGrowth != null ? NumericUtil.round1(consensusGrowth * 100.0) : null)
                .historicalCagrPct(historicalCagr != null ? NumericUtil.round1(historicalCagr * 100.0) : null)
                .interpretation(buildInterpretation(impliedGrowthPct, zone, proxy, sensitivity))
                .isClipped(solver.isClipped())
                .build();
    }

    private List<FcfMarginSensitivityEntryDto> buildMarginSensitivity(
            ForwardReverseDcfRequest request,
            EarningsEstimateDto fy1,
            EarningsEstimateDto fy2,
            double fy1Stub,
            double stage2BaseFcf,
            double t1,
            double t2
    ) {
        EarningsEstimateDto stage2Est = fy2 != null ? fy2 : fy1;
        if (stage2Est == null || stage2Est.revenueEstimateAverage() == null
                || stage2Est.revenueEstimateAverage().doubleValue() <= 0) {
            return List.of();
        }
        double stage2Revenue = stage2Est.revenueEstimateAverage().doubleValue();
        double baseMargin = stage2BaseFcf / stage2Revenue;
        if (baseMargin <= 0.01) {
            return List.of();
        }
        Double qtrMargin = resolveLatestQuarterlyFcfMargin(request.quarterlyRatios());
        double lowMargin = (qtrMargin != null && qtrMargin > 0.01 && qtrMargin < baseMargin - 0.01)
                ? qtrMargin
                : Math.max(0.05, baseMargin * 0.80);
        String lowLabel = (qtrMargin != null && qtrMargin > 0.01 && qtrMargin < baseMargin - 0.01)
                ? "Latest Quarter FCF Margin"
                : "Conservative / Stressed FCF Margin";
        double midMargin = (lowMargin + baseMargin) / 2.0;

        List<FcfMarginSensitivityEntryDto> rows = new ArrayList<>();
        rows.add(buildSensitivityEntry(lowLabel, lowMargin, baseMargin, stage2Revenue, request.marketCap(), fy1Stub, t1, t2));
        rows.add(buildSensitivityEntry("Blended Mid-Cycle FCF Margin", midMargin, baseMargin, stage2Revenue, request.marketCap(), fy1Stub, t1, t2));
        rows.add(buildSensitivityEntry("Normalized Proxy FCF Margin", baseMargin, baseMargin, stage2Revenue, request.marketCap(), fy1Stub, t1, t2));
        return rows;
    }

    private FcfMarginSensitivityEntryDto buildSensitivityEntry(
            String label,
            double targetMargin,
            double baseMargin,
            double stage2Revenue,
            double marketCap,
            double baseFy1Stub,
            double t1,
            double t2
    ) {
        double impliedFy2Fcf = stage2Revenue * targetMargin;
        double scaledFy1Stub = baseFy1Stub * (targetMargin / baseMargin);
        ForwardDcfSolverResult solved = solveImpliedGrowthRate(marketCap, scaledFy1Stub, impliedFy2Fcf, t1, t2);
        Float growthPct = NumericUtil.round1(solved.impliedGrowth() * 100.0);

        return FcfMarginSensitivityEntryDto.builder()
                .scenarioLabel(label)
                .assumedFcfMarginPct(NumericUtil.round1(targetMargin * 100.0))
                .impliedFy2FcfUsd(Math.round(impliedFy2Fcf * 100.0) / 100.0)
                .impliedGrowthYears3To10Pct(growthPct)
                .valuationZone(determineUnifiedValuationZone(growthPct))
                .build();
    }

    private Double resolveLatestQuarterlyFcfMargin(List<CompanyFinancialRatiosDto> quarterlyRatios) {
        if (quarterlyRatios == null || quarterlyRatios.isEmpty()) {
            return null;
        }
        return quarterlyRatios.stream()
                .filter(r -> r != null && r.reportDate() != null && r.fcfMargin() != null)
                .max(Comparator.comparing(CompanyFinancialRatiosDto::reportDate))
                .map(r -> r.fcfMargin().doubleValue())
                .orElse(null);
    }

    private ForwardDcfSolverResult solveImpliedGrowthRate(
            double marketCap,
            double fy1Stub,
            double fy2Proxy,
            double t1,
            double t2
    ) {
        double low = SOLVER_LOW;
        double high = SOLVER_HIGH;
        if (calculateTotalPv(fy1Stub, fy2Proxy, t1, t2, low) >= marketCap) {
            return new ForwardDcfSolverResult(low, true);
        }
        if (calculateTotalPv(fy1Stub, fy2Proxy, t1, t2, high) <= marketCap) {
            return new ForwardDcfSolverResult(high, true);
        }
        return bisectGrowthRate(marketCap, fy1Stub, fy2Proxy, t1, t2, low, high);
    }

    private ForwardDcfSolverResult bisectGrowthRate(
            double marketCap,
            double fy1Stub,
            double fy2Proxy,
            double t1,
            double t2,
            double low,
            double high
    ) {
        for (int iter = 0; iter < 100; iter++) {
            double mid = (low + high) / 2.0;
            double pvMid = calculateTotalPv(fy1Stub, fy2Proxy, t1, t2, mid);
            if (Math.abs(pvMid - marketCap) < SOLVER_TOLERANCE * marketCap || (high - low) < 0.0001) {
                return new ForwardDcfSolverResult(mid, false);
            }
            if (pvMid > marketCap) {
                high = mid;
            } else {
                low = mid;
            }
        }
        return new ForwardDcfSolverResult((low + high) / 2.0, false);
    }

    private double calculateTotalPv(double fy1Stub, double fy2Proxy, double t1, double t2, double g) {
        double pvStage1 = (fy1Stub / Math.pow(1.0 + DEFAULT_DISCOUNT_RATE, t1))
                + (fy2Proxy / Math.pow(1.0 + DEFAULT_DISCOUNT_RATE, t2));
        double pvStage2 = 0.0;
        double lastFcf = fy2Proxy;
        for (int year = 3; year <= DEFAULT_PROJECTION_YEARS; year++) {
            lastFcf *= (1.0 + g);
            pvStage2 += lastFcf / Math.pow(1.0 + DEFAULT_DISCOUNT_RATE, t2 + (year - 2));
        }
        double terminalValue = (lastFcf * (1.0 + DEFAULT_TERMINAL_GROWTH))
                / Math.max(0.01, DEFAULT_DISCOUNT_RATE - DEFAULT_TERMINAL_GROWTH);
        double pvTerminal = terminalValue / Math.pow(1.0 + DEFAULT_DISCOUNT_RATE, t2 + (DEFAULT_PROJECTION_YEARS - 2));
        return pvStage1 + pvStage2 + pvTerminal;
    }

    /**
     * Unified Reverse DCF legend across both single-stage and two-stage models:
     * <= 6.0% DEEP_DISCOUNT, < 12.0% FAIR, <= 20.0% AGGRESSIVE, > 20.0% SPECULATIVE.
     */
    private ValuationZone determineUnifiedValuationZone(Float impliedGrowthPct) {
        if (impliedGrowthPct == null) {
            return ValuationZone.FAIR;
        }
        if (impliedGrowthPct <= 6.0f) {
            return ValuationZone.DEEP_DISCOUNT;
        }
        if (impliedGrowthPct < 12.0f) {
            return ValuationZone.FAIR;
        }
        if (impliedGrowthPct <= 20.0f) {
            return ValuationZone.AGGRESSIVE;
        }
        return ValuationZone.SPECULATIVE;
    }

    private String buildInterpretation(
            Float impliedG,
            ValuationZone zone,
            MultiYearFcfProxyResult proxy,
            List<FcfMarginSensitivityEntryDto> sensitivity
    ) {
        String base = String.format(
                Locale.ROOT,
                "Two-stage Reverse DCF implies %.1f%% annualized FCF growth in Years 3-10 (%s zone) at normalized FY2 FCF proxy of $%.1fB.",
                impliedG, zone, proxy.fy2FcfProxy() != null ? proxy.fy2FcfProxy() / 1e9 : 0.0
        );
        if (sensitivity != null && sensitivity.size() >= 2) {
            FcfMarginSensitivityEntryDto lowRow = sensitivity.getFirst();
            return base + String.format(
                    Locale.ROOT,
                    " At %s (%.1f%%), FY2 FCF is $%.1fB and required Years 3-10 growth rises to %.1f%% (%s zone).",
                    lowRow.scenarioLabel(),
                    lowRow.assumedFcfMarginPct(),
                    lowRow.impliedFy2FcfUsd() / 1e9,
                    lowRow.impliedGrowthYears3To10Pct(),
                    lowRow.valuationZone()
            );
        }
        return base;
    }

    private Float computeConsensusCagr(EarningsEstimateDto fy2, CompanyFinancialRatiosDto ttmRatio, double t2) {
        if (fy2 == null || fy2.revenueEstimateAverage() == null
                || ttmRatio == null || ttmRatio.revenue() == null || ttmRatio.revenue() <= 0) {
            return null;
        }
        double rev0 = ttmRatio.revenue().doubleValue();
        double rev2 = fy2.revenueEstimateAverage().doubleValue();
        if (rev2 <= rev0) {
            return 0.0f;
        }
        double horizonYears = Math.max(0.5, t2);
        return (float) (Math.pow(rev2 / rev0, 1.0 / horizonYears) - 1.0);
    }

    private Float computeHistoricalFcfCagr(List<CompanyFinancialRatiosDto> annual) {
        if (annual == null || annual.size() < 2) {
            return null;
        }
        List<CompanyFinancialRatiosDto> valid = annual.stream()
                .filter(r -> r.freeCashFlow() != null && r.freeCashFlow() > 0 && r.fiscalYear() > 0)
                .sorted(Comparator.comparing(CompanyFinancialRatiosDto::fiscalYear))
                .toList();
        if (valid.size() < 2) {
            return null;
        }
        int years = valid.getLast().fiscalYear() - valid.getFirst().fiscalYear();
        if (years <= 0) {
            return null;
        }
        return (float) (Math.pow(valid.getLast().freeCashFlow() / valid.getFirst().freeCashFlow(), 1.0 / years) - 1.0);
    }

    private double computeTimeHorizonYears(LocalDate start, LocalDate target, double defaultYears) {
        if (start == null || target == null || !target.isAfter(start)) {
            return defaultYears;
        }
        return Math.max(0.1, ChronoUnit.DAYS.between(start, target) / 365.25);
    }

    private Double resolveDilutedShares(CompanyFinancialRatiosDto ttmRatio, Double marketCap, Float sharePrice) {
        if (ttmRatio != null && ttmRatio.sharesStabilized() != null && ttmRatio.sharesStabilized() > 0) {
            return ttmRatio.sharesStabilized().doubleValue();
        }
        if (marketCap != null && sharePrice != null && sharePrice > 0) {
            return marketCap / sharePrice.doubleValue();
        }
        return null;
    }

    private EarningsEstimateDto resolveFy1(ForwardReverseDcfRequest request) {
        if (request.fy1() != null) {
            return request.fy1();
        }
        List<EarningsEstimateDto> fyEstimates = filterFiscalYears(request.estimates(), request.analysisDate());
        return !fyEstimates.isEmpty() ? fyEstimates.getFirst() : null;
    }

    private EarningsEstimateDto resolveFy2(ForwardReverseDcfRequest request) {
        if (request.fy2() != null) {
            return request.fy2();
        }
        List<EarningsEstimateDto> fyEstimates = filterFiscalYears(request.estimates(), request.analysisDate());
        return fyEstimates.size() > 1 ? fyEstimates.get(1) : null;
    }

    private List<EarningsEstimateDto> filterFiscalYears(List<EarningsEstimateDto> list, LocalDate date) {
        return list.stream()
                .filter(this::isAnnualHorizon)
                .filter(e -> e.date() != null && (date == null || !e.date().isBefore(date)))
                .sorted(Comparator.comparing(EarningsEstimateDto::date))
                .toList();
    }

    private boolean isAnnualHorizon(EarningsEstimateDto estimate) {
        if (estimate.horizon() == null) {
            return false;
        }
        String normalized = estimate.horizon().toLowerCase(Locale.ROOT);
        return normalized.contains("year") || normalized.contains("annual");
    }
}
