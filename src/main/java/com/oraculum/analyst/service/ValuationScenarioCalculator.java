package com.oraculum.analyst.service;

import com.oraculum.analyst.dto.ValuationScenarioDto;
import com.oraculum.analyst.dto.ValuationScenarioDto.SensitivityCell;
import com.oraculum.analyst.dto.ValuationScenarioDto.SensitivityMatrix;
import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
public class ValuationScenarioCalculator {

    public static final List<Float> DEFAULT_REFERENCE_MULTIPLES = List.of(12.0f, 16.0f, 20.0f);
    public static final String SENSITIVITY_DISCLAIMER =
            "Illustrative sensitivity calculation only, not a price forecast or target.";

    private static Float round2(double val) {
        return (float) (Math.round(val * 100.0) / 100.0);
    }

    private static Float round1(float val) {
        return Math.round(val * 10.0f) / 10.0f;
    }

    public ValuationScenarioDto calculate(
            Float currentSharePrice,
            List<EarningsEstimateDto> estimates,
            LocalDate analysisDate,
            ReverseDcfDto reverseDcfResult
    ) {
        return calculate(currentSharePrice, estimates, analysisDate, reverseDcfResult, DEFAULT_REFERENCE_MULTIPLES);
    }

    public ValuationScenarioDto calculate(
            Float currentSharePrice,
            List<EarningsEstimateDto> estimates,
            LocalDate analysisDate,
            ReverseDcfDto reverseDcfResult,
            List<Float> referenceMultiples
    ) {
        if (estimates == null || estimates.isEmpty() || currentSharePrice == null || currentSharePrice <= 0) {
            return ValuationScenarioDto.empty();
        }

        List<EarningsEstimateDto> fyEstimates = filterFiscalYears(estimates, analysisDate);
        List<EarningsEstimateDto> qEstimates = filterFiscalQuarters(estimates, analysisDate);

        EarningsEstimateDto fy1 = !fyEstimates.isEmpty() ? fyEstimates.get(0) : null;
        EarningsEstimateDto fy2 = fyEstimates.size() > 1 ? fyEstimates.get(1) : null;

        Float forwardPeFy1 = calculateForwardPe(currentSharePrice, fy1);
        Float forwardPeFy2 = calculateForwardPe(currentSharePrice, fy2);

        EarningsEstimateDto nextQ = !qEstimates.isEmpty() ? qEstimates.getFirst() : null;
        LocalDate nextQuarterDate = nextQ != null ? nextQ.date() : null;
        Float nextQuarterEps = nextQ != null && nextQ.epsEstimateAverage() != null ? nextQ.epsEstimateAverage().floatValue() : null;
        Float nextQuarterRev = nextQ != null && nextQ.revenueEstimateAverage() != null ? nextQ.revenueEstimateAverage().floatValue() : null;

        EpsReconciliation reconciliation = reconcileQuarterlyVsAnnual(fy1, qEstimates);
        SensitivityMatrix sensitivityMatrix = buildSensitivityMatrix(currentSharePrice, fy2 != null ? fy2 : fy1, referenceMultiples);
        boolean flagDivergence = checkReverseDcfDivergence(reverseDcfResult, forwardPeFy1);

        return new ValuationScenarioDto(
                currentSharePrice,
                forwardPeFy1,
                forwardPeFy2,
                nextQuarterDate,
                nextQuarterEps,
                nextQuarterRev,
                reconciliation.divergence(),
                reconciliation.note(),
                sensitivityMatrix,
                flagDivergence
        );
    }

    private Float calculateForwardPe(Float price, EarningsEstimateDto estimate) {
        if (estimate == null || estimate.epsEstimateAverage() == null) {
            return null;
        }
        double eps = estimate.epsEstimateAverage().doubleValue();
        if (eps <= 0.001) {
            return null;
        }
        return round2(price / eps);
    }

    private List<EarningsEstimateDto> filterFiscalYears(List<EarningsEstimateDto> estimates, LocalDate analysisDate) {
        return estimates.stream()
                .filter(this::isFiscalYear)
                .filter(e -> e.date() != null && (analysisDate == null || !e.date().isBefore(analysisDate)))
                .sorted(Comparator.comparing(EarningsEstimateDto::date))
                .toList();
    }

    private List<EarningsEstimateDto> filterFiscalQuarters(List<EarningsEstimateDto> estimates, LocalDate analysisDate) {
        return estimates.stream()
                .filter(this::isFiscalQuarter)
                .filter(e -> e.date() != null && (analysisDate == null || !e.date().isBefore(analysisDate)))
                .sorted(Comparator.comparing(EarningsEstimateDto::date))
                .toList();
    }

    private boolean isFiscalYear(EarningsEstimateDto dto) {
        if (dto.horizon() == null) return false;
        String h = dto.horizon().trim().toLowerCase();
        return h.contains("year") || h.contains("annual");
    }

    private boolean isFiscalQuarter(EarningsEstimateDto dto) {
        if (dto.horizon() == null) return false;
        String h = dto.horizon().trim().toLowerCase();
        return h.contains("quarter");
    }

    private EpsReconciliation reconcileQuarterlyVsAnnual(EarningsEstimateDto fy, List<EarningsEstimateDto> quarters) {
        if (fy == null || fy.epsEstimateAverage() == null || quarters == null || quarters.isEmpty()) {
            return new EpsReconciliation(false, null);
        }
        double fyAvg = fy.epsEstimateAverage().doubleValue();
        if (fyAvg <= 0.001) {
            return new EpsReconciliation(false, null);
        }

        double quarterlySum = quarters.stream()
                .filter(q -> q.date() != null && !q.date().isAfter(fy.date()))
                .map(EarningsEstimateDto::epsEstimateAverage)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();

        if (quarterlySum <= 0.001) {
            return new EpsReconciliation(false, null);
        }

        double diff = Math.abs(quarterlySum - fyAvg);
        double diffPct = (diff / fyAvg) * 100.0;
        if (diffPct > 8.0) {
            String note = String.format(Locale.ROOT, "Quarterly sum (%.2f) deviates by %.1f%% from FY consensus (%.2f); verify GAAP actual vs non-GAAP consensus alignment.",
                    quarterlySum, diffPct, fyAvg);
            return new EpsReconciliation(true, note);
        }
        return new EpsReconciliation(false, null);
    }

    private SensitivityMatrix buildSensitivityMatrix(Float currentPrice, EarningsEstimateDto target, List<Float> multiples) {
        if (target == null) {
            return null;
        }
        Float epsLow = target.epsEstimateLow() != null ? target.epsEstimateLow().floatValue() : null;
        Float epsAvg = target.epsEstimateAverage() != null ? target.epsEstimateAverage().floatValue() : null;
        Float epsHigh = target.epsEstimateHigh() != null ? target.epsEstimateHigh().floatValue() : null;

        List<Float> refs = (multiples != null && !multiples.isEmpty()) ? multiples : DEFAULT_REFERENCE_MULTIPLES;
        List<SensitivityCell> cells = new ArrayList<>();

        for (Float multiple : refs) {
            addCellIfPresent(cells, multiple, "LOW", epsLow, currentPrice);
            addCellIfPresent(cells, multiple, "AVG", epsAvg, currentPrice);
            addCellIfPresent(cells, multiple, "HIGH", epsHigh, currentPrice);
        }

        String targetLabel = target.date() != null ? target.date().toString() : "Next FY";
        return new SensitivityMatrix(targetLabel, epsLow, epsAvg, epsHigh, refs, cells, SENSITIVITY_DISCLAIMER);
    }

    private void addCellIfPresent(List<SensitivityCell> cells, Float multiple, String scenario, Float eps, Float currentPrice) {
        if (multiple == null || eps == null || currentPrice == null || currentPrice <= 0) {
            return;
        }
        float implied = round2(multiple * eps);
        float pctMove = round1(((implied - currentPrice) / currentPrice) * 100.0f);
        cells.add(new SensitivityCell(multiple, scenario, eps, implied, pctMove));
    }

    private boolean checkReverseDcfDivergence(ReverseDcfDto reverseDcf, Float forwardPe) {
        if (reverseDcf == null || forwardPe == null) {
            return false;
        }
        boolean isAggressive = (reverseDcf.interpretation() != null && reverseDcf.interpretation().toUpperCase().contains("AGGRESSIVE"))
                || (reverseDcf.impliedFcfGrowthRatePct() != null && reverseDcf.impliedFcfGrowthRatePct() > 25.0);
        return isAggressive && forwardPe < 25.0f;
    }

    private record EpsReconciliation(boolean divergence, String note) {
    }
}
