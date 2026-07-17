package com.oraculum.analyst.service.calculator;

import com.oraculum.analyst.dto.ReverseDcfResult;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.company.api.dto.SharePriceSignalDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ReverseDcfCalculator {

    private static final float DEFAULT_DISCOUNT_RATE = 0.10f;
    private static final float DEFAULT_TERMINAL_GROWTH = 0.03f;
    private static final int DEFAULT_PROJECTION_YEARS = 10;

    public ReverseDcfResult calculate(List<SharePriceSignalDto> dailySignals, Map<StatementVariant, List<CompanyFinancialRatiosDto>> ratios) {
        if (dailySignals == null || dailySignals.isEmpty() || ratios == null || ratios.isEmpty()) {
            return null;
        }

        Float marketCap = dailySignals.getFirst().marketCapitalization();
        Float currentFcf = extractLatestTtmFcf(ratios.get(StatementVariant.TTM));
        List<CompanyFinancialRatiosDto> annualRatios = ratios.get(StatementVariant.ANNUAL);

        if (marketCap == null || currentFcf == null || marketCap <= 0 || currentFcf <= 0) {
            return null;
        }

        float fcfYield = currentFcf / marketCap;
        float impliedG = solveImpliedGrowthRate(marketCap, currentFcf, DEFAULT_DISCOUNT_RATE, DEFAULT_PROJECTION_YEARS, DEFAULT_TERMINAL_GROWTH);
        Float historicalCagr = computeHistoricalFcfCagr(annualRatios);

        String interpretation = buildInterpretation(impliedG, historicalCagr, marketCap, currentFcf);

        return new ReverseDcfResult(
                marketCap,
                currentFcf,
                fcfYield * 100,
                DEFAULT_DISCOUNT_RATE * 100,
                DEFAULT_PROJECTION_YEARS,
                DEFAULT_TERMINAL_GROWTH * 100,
                impliedG * 100,
                historicalCagr != null ? historicalCagr * 100 : null,
                interpretation
        );
    }

    private Float extractLatestTtmFcf(List<CompanyFinancialRatiosDto> ttmRatios) {
        if (ttmRatios == null || ttmRatios.isEmpty()) return null;
        List<CompanyFinancialRatiosDto> sorted = new ArrayList<>(ttmRatios);
        sorted.sort(CompanyFinancialRatiosDto.getComparator().reversed());
        return sorted.getFirst().freeCashFlow();
    }

    private float solveImpliedGrowthRate(float marketCap, float fcf, float r, int n, float gTerminal) {
        float low = -0.50f;
        float high = 0.80f;
        float tolerance = 0.0001f;

        for (int i = 0; i < 100; i++) {
            float mid = (low + high) / 2;
            float val = computeModelValue(fcf, mid, r, n, gTerminal);
            if (Math.abs(val - marketCap) < tolerance * marketCap) {
                return mid;
            }
            if (val > marketCap) {
                high = mid;
            } else {
                low = mid;
            }
        }
        return (low + high) / 2;
    }

    private float computeModelValue(float fcf, float g, float r, int n, float gTerminal) {
        float sum = 0f;
        float tempFcf = fcf;
        float discountFactor = 1 + r;

        for (int i = 1; i <= n; i++) {
            tempFcf *= (1 + g);
            sum += (float) (tempFcf / Math.pow(discountFactor, i));
        }

        double terminalVal = (tempFcf * (1 + gTerminal)) / (r - gTerminal);
        sum += (float) (terminalVal / Math.pow(discountFactor, n));

        return sum;
    }

    private Float computeHistoricalFcfCagr(List<CompanyFinancialRatiosDto> annualRatios) {
        if (annualRatios == null || annualRatios.isEmpty()) return null;

        List<CompanyFinancialRatiosDto> sorted = annualRatios.stream()
                .filter(Objects::nonNull)
                .filter(r -> r.freeCashFlow() != null)
                .sorted(Comparator.comparing(CompanyFinancialRatiosDto::fiscalYear))
                .toList();

        if (sorted.size() < 2) return null;

        CompanyFinancialRatiosDto oldest = sorted.getFirst();
        CompanyFinancialRatiosDto newest = sorted.getLast();

        float oldestFcf = oldest.freeCashFlow();
        float newestFcf = newest.freeCashFlow();
        int years = newest.fiscalYear() - oldest.fiscalYear();

        if (years <= 0 || oldestFcf <= 0 || newestFcf <= 0) return null;

        return (float) (Math.pow((double) newestFcf / oldestFcf, 1.0 / years) - 1.0);
    }

    private String buildInterpretation(float impliedG, Float historicalCagr, float marketCap, float fcf) {
        String base = String.format(java.util.Locale.US, "At the current market capitalization of $%.2fB and TTM Free Cash Flow of $%.2fB (FCF Yield: %.2f%%), " +
                "the market implies that the company must grow its Free Cash Flow at an average annual rate of %.1f%% for the next %d years (assuming a %.1f%% discount rate and %.1f%% terminal growth).",
                marketCap / 1_000_000_000f, fcf / 1_000_000_000f, (fcf / marketCap) * 100, impliedG * 100, DEFAULT_PROJECTION_YEARS, DEFAULT_DISCOUNT_RATE * 100, DEFAULT_TERMINAL_GROWTH * 100);

        if (historicalCagr != null) {
            base += String.format(java.util.Locale.US, " For comparison, the company's historical annual FCF growth (CAGR) was %.1f%% over the analyzed period.", historicalCagr * 100);
        } else {
            base += " (No historical FCF CAGR comparison available due to insufficient positive annual FCF history).";
        }
        return base;
    }
}
