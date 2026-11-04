package com.oraculum.analyst.service.valuation;

import com.oraculum.analyst.dto.valuation.OperatingEarningsSummaryDto;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.util.NumericUtil;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class OperatingEarningsCalculator {

    private static final float DEFAULT_EFFECTIVE_TAX_RATE = 0.18f;
    private static final float MIN_NORMAL_TAX_RATE = 0.05f;
    private static final float MAX_NORMAL_TAX_RATE = 0.35f;

    public OperatingEarningsSummaryDto calculate(Float sharePrice, CompanyFinancialRatiosDto ttmRatio) {
        if (!hasRequiredInputs(sharePrice, ttmRatio)) {
            return null;
        }
        float operatingIncome = ttmRatio.revenue() * ttmRatio.operatingMargin();
        float effectiveTaxRate = resolveEffectiveTaxRate(operatingIncome, ttmRatio);
        float operatingNetIncome = operatingIncome * (1.0f - effectiveTaxRate);
        Float operatingEps = NumericUtil.round2(operatingNetIncome / ttmRatio.sharesStabilized());

        Float operatingPe = operatingEps > 0.001f ? NumericUtil.round2(sharePrice / operatingEps) : null;
        Float gaapEps = ttmRatio.earningsPerShare();
        Float gaapPe = (gaapEps != null && gaapEps > 0.001f) ? NumericUtil.round2(sharePrice / gaapEps) : null;

        return buildSummary(operatingEps, operatingPe, gaapPe, effectiveTaxRate, ttmRatio);
    }

    private boolean hasRequiredInputs(Float sharePrice, CompanyFinancialRatiosDto ttmRatio) {
        if (sharePrice == null || sharePrice <= 0 || ttmRatio == null) {
            return false;
        }
        return ttmRatio.operatingMargin() != null
                && ttmRatio.revenue() != null
                && ttmRatio.sharesStabilized() != null
                && ttmRatio.sharesStabilized() > 0;
    }

    private float resolveEffectiveTaxRate(float operatingIncome, CompanyFinancialRatiosDto ttmRatio) {
        Float netIncome = ttmRatio.netIncome();
        Float nonOpSharePct = ttmRatio.nonOperatingIncomeSharePretaxPct();
        if (operatingIncome <= 0 || netIncome == null || netIncome <= 0
                || nonOpSharePct == null || Math.abs(nonOpSharePct) >= 60.0f) {
            return DEFAULT_EFFECTIVE_TAX_RATE;
        }
        double ebt = operatingIncome / (1.0 - (nonOpSharePct / 100.0));
        if (ebt <= 0) {
            return DEFAULT_EFFECTIVE_TAX_RATE;
        }
        double impliedTax = 1.0 - (netIncome / ebt);
        if (impliedTax < 0.0 || impliedTax > 0.50) {
            return DEFAULT_EFFECTIVE_TAX_RATE;
        }
        return NumericUtil.round2(Math.clamp((float) impliedTax, MIN_NORMAL_TAX_RATE, MAX_NORMAL_TAX_RATE));
    }

    private OperatingEarningsSummaryDto buildSummary(
            Float operatingEps,
            Float operatingPe,
            Float gaapPe,
            float effectiveTaxRate,
            CompanyFinancialRatiosDto ttmRatio
    ) {
        Float nonOpShare = ttmRatio.nonOperatingIncomeSharePretaxPct();
        boolean highNonOp = Integer.valueOf(1).equals(ttmRatio.flagHighNonOperatingIncome())
                || (nonOpShare != null && Math.abs(nonOpShare) > 10.0f);

        return OperatingEarningsSummaryDto.builder()
                .operatingEps(operatingEps)
                .operatingPe(operatingPe)
                .gaapPe(gaapPe)
                .effectiveTaxRate(effectiveTaxRate)
                .nonOperatingIncomePretaxPct(nonOpShare)
                .flagHighNonOperatingIncome(highNonOp)
                .note(buildNote(operatingPe, gaapPe, nonOpShare, highNonOp))
                .build();
    }

    private String buildNote(Float opPe, Float gaapPe, Float nonOpShare, boolean highNonOp) {
        if (opPe == null || gaapPe == null) {
            return null;
        }
        if (highNonOp) {
            return String.format(
                    Locale.ROOT,
                    "Elevated non-operating items (%.1f%% of pretax income). Operating P/E of %.1fx contrasts with GAAP P/E of %.1fx.",
                    nonOpShare != null ? nonOpShare : 0.0f, opPe, gaapPe
            );
        }
        return String.format(
                Locale.ROOT,
                "Operating P/E is %.1fx (GAAP P/E %.1fx). Non-operating distortion is minimal.",
                opPe, gaapPe
        );
    }
}
