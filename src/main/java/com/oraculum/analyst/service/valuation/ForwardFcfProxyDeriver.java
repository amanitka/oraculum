package com.oraculum.analyst.service.valuation;

import com.oraculum.analyst.dto.valuation.ForwardFcfProxyRequest;
import com.oraculum.analyst.dto.valuation.MultiYearFcfProxyResult;
import com.oraculum.analyst.dto.valuation.YearFcfProxyResult;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import com.oraculum.util.NumericUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class ForwardFcfProxyDeriver {

    public MultiYearFcfProxyResult derive(ForwardFcfProxyRequest request) {
        if (request == null) {
            return null;
        }
        double medianMargin = calculateMedianFcfMargin(request.annualRatios(), request.ttmRatio());
        double medianConversion = calculateMedianFcfConversion(request.annualRatios(), request.ttmRatio());

        YearFcfProxyResult proxy1 = deriveSingleYear(request.fy1(), medianMargin, medianConversion, request.dilutedShares());
        YearFcfProxyResult proxy2 = deriveSingleYear(request.fy2(), medianMargin, medianConversion, request.dilutedShares());

        return buildMultiYearResult(proxy1, proxy2, medianMargin, medianConversion);
    }

    private MultiYearFcfProxyResult buildMultiYearResult(
            YearFcfProxyResult proxy1,
            YearFcfProxyResult proxy2,
            double medianMargin,
            double medianConversion
    ) {
        boolean flagDiv = (proxy1 != null && proxy1.isDivergent()) || (proxy2 != null && proxy2.isDivergent());
        float maxDiv = Math.max(
                proxy1 != null && proxy1.divergencePct() != null ? proxy1.divergencePct() : 0.0f,
                proxy2 != null && proxy2.divergencePct() != null ? proxy2.divergencePct() : 0.0f
        );
        return MultiYearFcfProxyResult.builder()
                .fy1FcfProxy(proxy1 != null ? proxy1.fcfProxy() : null)
                .fy2FcfProxy(proxy2 != null ? proxy2.fcfProxy() : null)
                .derivationMethod(buildMethodDescription(medianMargin, medianConversion))
                .flagDivergence(flagDiv)
                .maxDivergencePct(maxDiv)
                .build();
    }

    private YearFcfProxyResult deriveSingleYear(
            EarningsEstimateDto est,
            double medianMargin,
            double medianConversion,
            Double dilutedShares
    ) {
        if (est == null) {
            return null;
        }
        Double pathA = computePathA(est.revenueEstimateAverage(), medianMargin);
        Double pathB = computePathB(est.epsEstimateAverage(), dilutedShares, medianConversion);
        return reconcilePaths(pathA, pathB);
    }

    private Double computePathA(BigDecimal revEst, double medianMargin) {
        if (revEst == null || revEst.doubleValue() <= 0 || medianMargin <= 0) {
            return null;
        }
        return revEst.doubleValue() * medianMargin;
    }

    private Double computePathB(BigDecimal epsEst, Double shares, double medianConversion) {
        if (epsEst == null || epsEst.doubleValue() <= 0 || shares == null || shares <= 0 || medianConversion <= 0) {
            return null;
        }
        return epsEst.doubleValue() * shares * medianConversion;
    }

    private YearFcfProxyResult reconcilePaths(Double fcfA, Double fcfB) {
        if (fcfA == null && fcfB == null) {
            return null;
        }
        if (fcfA == null) {
            return singlePathResult(fcfB, null, fcfB);
        }
        if (fcfB == null) {
            return singlePathResult(fcfA, fcfA, null);
        }
        double min = Math.min(fcfA, fcfB);
        double max = Math.max(fcfA, fcfB);
        float divPct = min > 0 ? (float) (((max - min) / min) * 100.0) : 0.0f;
        double chosen = divPct <= 15.0f ? (fcfA + fcfB) / 2.0 : min;

        return YearFcfProxyResult.builder()
                .fcfProxy(chosen)
                .fcfPathA(fcfA)
                .fcfPathB(fcfB)
                .divergencePct(divPct)
                .isDivergent(divPct > 30.0f)
                .build();
    }

    private YearFcfProxyResult singlePathResult(Double chosen, Double pathA, Double pathB) {
        return YearFcfProxyResult.builder()
                .fcfProxy(chosen)
                .fcfPathA(pathA)
                .fcfPathB(pathB)
                .divergencePct(0.0f)
                .isDivergent(false)
                .build();
    }

    private double calculateMedianFcfMargin(List<CompanyFinancialRatiosDto> annual, CompanyFinancialRatiosDto ttm) {
        List<Double> margins = new ArrayList<>();
        if (annual != null) {
            annual.stream()
                    .map(CompanyFinancialRatiosDto::fcfMargin)
                    .filter(m -> m != null && m > 0.001f)
                    .map(Float::doubleValue)
                    .forEach(margins::add);
        }
        if (ttm != null && ttm.fcfMargin() != null && ttm.fcfMargin() > 0.001f) {
            margins.add(ttm.fcfMargin().doubleValue());
        }
        return NumericUtil.medianDouble(margins, 0.20);
    }

    private double calculateMedianFcfConversion(List<CompanyFinancialRatiosDto> annual, CompanyFinancialRatiosDto ttm) {
        List<Double> ratios = new ArrayList<>();
        addValidConversion(ratios, ttm);
        if (annual != null) {
            annual.forEach(r -> addValidConversion(ratios, r));
        }
        return NumericUtil.medianDouble(ratios, 0.80);
    }

    private void addValidConversion(List<Double> list, CompanyFinancialRatiosDto dto) {
        if (dto != null && dto.freeCashFlow() != null && dto.netIncome() != null && dto.netIncome() > 0) {
            double conv = dto.freeCashFlow().doubleValue() / dto.netIncome().doubleValue();
            if (conv > 0.05 && conv < 3.0) {
                list.add(conv);
            }
        }
    }

    private String buildMethodDescription(double margin, double conversion) {
        return String.format(
                Locale.ROOT,
                "Dual-Path Proxy (Normalized FCF Margin: %.1f%%, FCF/NI Conversion: %.2fx)",
                margin * 100.0, conversion
        );
    }
}
