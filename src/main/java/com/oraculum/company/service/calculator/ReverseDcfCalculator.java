package com.oraculum.company.service.calculator;

import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.company.api.dto.SharePriceSignalDto;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ReverseDcfCalculator {

    private static final double DEFAULT_DISCOUNT_RATE = 0.10d;
    private static final double DEFAULT_TERMINAL_GROWTH = 0.03d;
    private static final int DEFAULT_PROJECTION_YEARS = 10;

    // Bounds for the implied-growth-rate solver. Kept wide, but the solver now
    // reports whether the answer landed on a bound rather than silently
    // returning a clipped value as if it were precise.
    private static final double SOLVER_LOW = -0.50d;
    private static final double SOLVER_HIGH = 0.80d;
    private static final double SOLVER_TOLERANCE = 0.0001d;
    private static final double BOUND_EPSILON = 0.001d; // within 0.1pp of a bound counts as "clipped"

    // Historical CAGR uses at most this many most-recent annual periods, not
    // the single earliest record available. Anchoring on whichever year
    // happens to be oldest (often a small, early-stage, or depressed base)
    // systematically inflates the CAGR and makes "historical growth" look
    // more favorable than it should for mature, high-growth names. A
    // trailing window is still a simplification, but it's far less sensitive
    // to which single data point happens to sit at the edge of history.
    private static final int CAGR_TRAILING_WINDOW_YEARS = 5;

    public ReverseDcfDto calculate(List<SharePriceSignalDto> dailySignals, Map<StatementVariant, List<CompanyFinancialRatiosDto>> ratios) {
        if (dailySignals == null || dailySignals.isEmpty() || ratios == null || ratios.isEmpty()) {
            return null;
        }

        Float marketCapBoxed = dailySignals.getFirst().marketCapitalization();
        Float currentFcfBoxed = extractLatestTtmFcf(ratios.get(StatementVariant.TTM));
        List<CompanyFinancialRatiosDto> annualRatios = ratios.get(StatementVariant.ANNUAL);

        if (marketCapBoxed == null || currentFcfBoxed == null || marketCapBoxed <= 0 || currentFcfBoxed <= 0) {
            return null;
        }

        double marketCap = marketCapBoxed;
        double currentFcf = currentFcfBoxed;

        double fcfYield = currentFcf / marketCap;
        SolverResult solverResult = solveImpliedGrowthRate(marketCap, currentFcf, DEFAULT_DISCOUNT_RATE, DEFAULT_PROJECTION_YEARS, DEFAULT_TERMINAL_GROWTH);
        HistoricalCagrResult historicalCagr = computeHistoricalFcfCagr(annualRatios);

        String interpretation = buildInterpretation(solverResult, historicalCagr, marketCap, currentFcf);

        String historicalPeriodSpan = historicalCagr != null ?
                String.format(Locale.US, "FY%d to FY%d (%d-year period)", historicalCagr.startFiscalYear(), historicalCagr.endFiscalYear(), historicalCagr.yearsSpanned())
                : null;

        return new ReverseDcfDto(
                marketCap,
                currentFcf,
                fcfYield * 100.0,
                DEFAULT_DISCOUNT_RATE * 100.0,
                DEFAULT_PROJECTION_YEARS,
                DEFAULT_TERMINAL_GROWTH * 100.0,
                solverResult.impliedGrowth() * 100.0,
                historicalCagr != null ? historicalCagr.cagr() * 100.0 : null,
                historicalPeriodSpan,
                solverResult.isClipped(),
                interpretation
        );
    }

    private Float extractLatestTtmFcf(List<CompanyFinancialRatiosDto> ttmRatios) {
        if (ttmRatios == null || ttmRatios.isEmpty()) return null;
        List<CompanyFinancialRatiosDto> sorted = new ArrayList<>(ttmRatios);
        sorted.sort(CompanyFinancialRatiosDto.getComparator().reversed());
        return sorted.getFirst().freeCashFlow();
    }

    private SolverResult solveImpliedGrowthRate(double marketCap, double fcf, double r, int n, double gTerminal) {
        double low = SOLVER_LOW;
        double high = SOLVER_HIGH;
        double mid = (low + high) / 2;

        for (int i = 0; i < 100; i++) {
            mid = (low + high) / 2;
            double val = computeModelValue(fcf, mid, r, n, gTerminal);
            if (Math.abs(val - marketCap) < SOLVER_TOLERANCE * marketCap) {
                break;
            }
            if (val > marketCap) {
                high = mid;
            } else {
                low = mid;
            }
        }

        boolean clippedLow = Math.abs(mid - SOLVER_LOW) < BOUND_EPSILON;
        boolean clippedHigh = Math.abs(mid - SOLVER_HIGH) < BOUND_EPSILON;
        return new SolverResult(mid, clippedLow, clippedHigh);
    }

    private double computeModelValue(double fcf, double g, double r, int n, double gTerminal) {
        double sum = 0d;
        double tempFcf = fcf;
        double discountFactor = 1 + r;

        for (int i = 1; i <= n; i++) {
            tempFcf *= (1 + g);
            sum += tempFcf / Math.pow(discountFactor, i);
        }

        double terminalVal = (tempFcf * (1 + gTerminal)) / (r - gTerminal);
        sum += terminalVal / Math.pow(discountFactor, n);

        return sum;
    }

    private HistoricalCagrResult computeHistoricalFcfCagr(List<CompanyFinancialRatiosDto> annualRatios) {
        if (annualRatios == null || annualRatios.isEmpty()) return null;

        List<CompanyFinancialRatiosDto> sorted = annualRatios.stream()
                .filter(Objects::nonNull)
                .filter(r -> r.freeCashFlow() != null)
                .sorted(Comparator.comparing(CompanyFinancialRatiosDto::fiscalYear))
                .toList();

        if (sorted.size() < 2) return null;

        // Use only the most recent CAGR_TRAILING_WINDOW_YEARS+1 data points
        // (enough for CAGR_TRAILING_WINDOW_YEARS of growth) instead of
        // anchoring on the single oldest record available.
        int windowSize = Math.min(sorted.size(), CAGR_TRAILING_WINDOW_YEARS + 1);
        List<CompanyFinancialRatiosDto> trailingWindow = sorted.subList(sorted.size() - windowSize, sorted.size());

        CompanyFinancialRatiosDto oldest = trailingWindow.getFirst();
        CompanyFinancialRatiosDto newest = trailingWindow.getLast();

        double oldestFcf = oldest.freeCashFlow();
        double newestFcf = newest.freeCashFlow();
        int years = newest.fiscalYear() - oldest.fiscalYear();

        if (years <= 0 || oldestFcf <= 0 || newestFcf <= 0) return null;

        double cagr = Math.pow(newestFcf / oldestFcf, 1.0 / years) - 1.0;
        return new HistoricalCagrResult(cagr, oldest.fiscalYear(), newest.fiscalYear(), years);
    }

    private String buildInterpretation(SolverResult solverResult, HistoricalCagrResult historicalCagr, double marketCap, double fcf) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US,
                "At the current market capitalization of $%.2fB and TTM Free Cash Flow of $%.2fB (FCF Yield: %.2f%%), " +
                        "the market implies that the company must grow its Free Cash Flow at an average annual rate of %.1f%% for the next %d years (assuming a %.1f%% discount rate and %.1f%% terminal growth).",
                marketCap / 1_000_000_000d, fcf / 1_000_000_000d, (fcf / marketCap) * 100,
                solverResult.impliedGrowth * 100, DEFAULT_PROJECTION_YEARS, DEFAULT_DISCOUNT_RATE * 100, DEFAULT_TERMINAL_GROWTH * 100));

        if (solverResult.isClipped()) {
            sb.append(String.format(Locale.US,
                    " Note: this figure sits at the edge of the modeled range (%.0f%% to %.0f%%) and should be treated as directional rather than precise.",
                    SOLVER_LOW * 100, SOLVER_HIGH * 100));
        }

        if (historicalCagr != null) {
            sb.append(String.format(Locale.US,
                    " For comparison, the company's historical annual FCF growth (CAGR) was %.1f%% from FY%d to FY%d (%d-year period).",
                    historicalCagr.cagr * 100, historicalCagr.startFiscalYear, historicalCagr.endFiscalYear, historicalCagr.yearsSpanned));
        } else {
            sb.append(" (No historical FCF CAGR comparison available due to insufficient positive annual FCF history).");
        }
        return sb.toString();
    }

    private record SolverResult(double impliedGrowth, boolean clippedToLowerBound, boolean clippedToUpperBound) {
        boolean isClipped() {
            return clippedToLowerBound || clippedToUpperBound;
        }
    }

    private record HistoricalCagrResult(double cagr, int startFiscalYear, int endFiscalYear, int yearsSpanned) {
    }
}
