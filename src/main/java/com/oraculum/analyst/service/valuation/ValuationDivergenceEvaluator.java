package com.oraculum.analyst.service.valuation;

import com.oraculum.analyst.dto.valuation.ForwardReverseDcfDto;
import com.oraculum.analyst.dto.valuation.MethodDivergenceResult;
import com.oraculum.analyst.dto.valuation.ReverseDcfSummaryDto;
import com.oraculum.company.api.dto.HistoricalValuationSummaryDto;
import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.company.api.dto.ReverseDcfMatrixCellDto;
import com.oraculum.company.api.dto.ValuationZone;
import com.oraculum.util.NumericUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class ValuationDivergenceEvaluator {

    public ReverseDcfSummaryDto buildReverseDcfSummary(ReverseDcfDto dcf) {
        if (dcf == null) {
            return null;
        }
        List<ReverseDcfMatrixCellDto> cells = (dcf.sensitivityMatrix() != null && dcf.sensitivityMatrix().cells() != null)
                ? dcf.sensitivityMatrix().cells()
                : List.of();
        ReverseDcfMatrixCellDto baseCell = cells.stream().filter(ReverseDcfMatrixCellDto::isBaseCase).findFirst().orElse(null);
        List<Float> impliedRates = cells.stream()
                .map(c -> NumericUtil.round2(c.impliedFcfGrowthPct()))
                .sorted()
                .toList();

        return ReverseDcfSummaryDto.builder()
                .baseCaseImpliedGrowthPct(dcf.impliedFcfGrowthRatePct() != null ? NumericUtil.round2(dcf.impliedFcfGrowthRatePct()) : null)
                .baseCaseValuationZone(baseCell != null && baseCell.valuationZone() != null ? baseCell.valuationZone().name() : null)
                .minImpliedGrowthPct(impliedRates.isEmpty() ? null : impliedRates.getFirst())
                .maxImpliedGrowthPct(impliedRates.isEmpty() ? null : impliedRates.getLast())
                .medianImpliedGrowthPct(NumericUtil.medianFloat(impliedRates))
                .aggressiveOrSpeculativeCellsPct(calculateAggressiveCellsPct(cells))
                .historicalFcfCagrPct(dcf.historicalFcfCagrPct() != null ? NumericUtil.round2(dcf.historicalFcfCagrPct()) : null)
                .build();
    }

    public MethodDivergenceResult evaluateMethodDivergence(
            ReverseDcfDto reverseDcf,
            Float forwardPe,
            List<HistoricalValuationSummaryDto> historicalValuation
    ) {
        return evaluateMethodDivergence(reverseDcf, null, forwardPe, historicalValuation);
    }

    public MethodDivergenceResult evaluateMethodDivergence(
            ReverseDcfDto reverseDcf,
            ForwardReverseDcfDto twoStageDcf,
            Float forwardPe,
            List<HistoricalValuationSummaryDto> historicalValuation
    ) {
        if (reverseDcf == null) {
            return new MethodDivergenceResult(false, null);
        }
        Optional<ReverseDcfMatrixCellDto> baseCellOpt = findBaseDcfCell(reverseDcf);
        boolean isAggressiveDcf = isAggressiveReverseDcf(reverseDcf, baseCellOpt.orElse(null));
        Integer pePercentile = findPeHistoricalSummary(historicalValuation)
                .map(HistoricalValuationSummaryDto::percentile10y)
                .orElse(null);
        boolean isLowPe = (forwardPe != null && forwardPe < 25.0f) || (pePercentile != null && pePercentile <= 25);

        if (!isAggressiveDcf || !isLowPe) {
            return new MethodDivergenceResult(false, null);
        }
        String zoneName = baseCellOpt.map(c -> c.valuationZone().name()).orElse("AGGRESSIVE");
        double impliedGrowth = reverseDcf.impliedFcfGrowthRatePct() != null ? reverseDcf.impliedFcfGrowthRatePct() : 0.0;

        if (isResolvedByTwoStageDcf(twoStageDcf)) {
            return buildReconciledNote(zoneName, impliedGrowth, twoStageDcf, forwardPe);
        }
        return buildMethodConflictNote(zoneName, impliedGrowth, forwardPe, pePercentile);
    }

    private boolean isResolvedByTwoStageDcf(ForwardReverseDcfDto twoStageDcf) {
        if (twoStageDcf == null || twoStageDcf.relativeValuationZone() == null) {
            return false;
        }
        ValuationZone zone = twoStageDcf.relativeValuationZone();
        return zone == ValuationZone.FAIR || zone == ValuationZone.DEEP_DISCOUNT;
    }

    private MethodDivergenceResult buildReconciledNote(
            String trailingZone,
            double trailingGrowth,
            ForwardReverseDcfDto twoStageDcf,
            Float forwardPe
    ) {
        String note = String.format(
                Locale.ROOT,
                "TRAILING VS FORWARD DCF RECONCILED: Trailing single-stage Reverse DCF appears in %s zone (%.1f%% 10Y FCF CAGR on TTM FCF), " +
                        "but Two-Stage Forward Reverse DCF resolves to %s zone (%.1f%% CAGR in Years 3-10) once consensus FY2 FCF is achieved, aligning with FY1 Forward P/E of %s.",
                trailingZone,
                trailingGrowth,
                twoStageDcf.relativeValuationZone().name(),
                twoStageDcf.impliedGrowthYears3To10Pct() != null ? twoStageDcf.impliedGrowthYears3To10Pct() : 0.0f,
                forwardPe != null ? String.format(Locale.ROOT, "%.1fx", forwardPe) : "N/A"
        );
        return new MethodDivergenceResult(false, note);
    }

    private MethodDivergenceResult buildMethodConflictNote(
            String zoneName,
            double impliedGrowth,
            Float forwardPe,
            Integer pePercentile
    ) {
        String note = String.format(
                Locale.ROOT,
                "METHOD CONFLICT: Multiples appear moderate/low (FY1 Forward P/E %s, 10Y P/E percentile %s), while Reverse DCF is in %s zone requiring %.1f%% 10Y FCF CAGR. Assess peak-cycle margin compression risk.",
                forwardPe != null ? String.format(Locale.ROOT, "%.1fx", forwardPe) : "N/A",
                pePercentile != null ? pePercentile + "%" : "N/A",
                zoneName,
                impliedGrowth
        );
        return new MethodDivergenceResult(true, note);
    }

    private Integer calculateAggressiveCellsPct(List<ReverseDcfMatrixCellDto> cells) {
        if (cells.isEmpty()) {
            return null;
        }
        long count = cells.stream()
                .filter(c -> c.valuationZone() == ValuationZone.AGGRESSIVE || c.valuationZone() == ValuationZone.SPECULATIVE)
                .count();
        return (int) Math.round((count * 100.0) / cells.size());
    }

    private Optional<ReverseDcfMatrixCellDto> findBaseDcfCell(ReverseDcfDto dcf) {
        if (dcf.sensitivityMatrix() == null || dcf.sensitivityMatrix().cells() == null) {
            return Optional.empty();
        }
        return dcf.sensitivityMatrix().cells().stream()
                .filter(ReverseDcfMatrixCellDto::isBaseCase)
                .findFirst();
    }

    private boolean isAggressiveReverseDcf(ReverseDcfDto dcf, ReverseDcfMatrixCellDto baseCell) {
        if (baseCell != null && (baseCell.valuationZone() == ValuationZone.AGGRESSIVE || baseCell.valuationZone() == ValuationZone.SPECULATIVE)) {
            return true;
        }
        boolean textAggressive = dcf.interpretation() != null
                && (dcf.interpretation().toUpperCase(Locale.ROOT).contains("AGGRESSIVE")
                || dcf.interpretation().toUpperCase(Locale.ROOT).contains("SPECULATIVE"));
        boolean rateHigh = dcf.impliedFcfGrowthRatePct() != null && dcf.impliedFcfGrowthRatePct() > 25.0;
        return textAggressive || rateHigh;
    }

    private Optional<HistoricalValuationSummaryDto> findPeHistoricalSummary(List<HistoricalValuationSummaryDto> list) {
        if (list == null) {
            return Optional.empty();
        }
        return list.stream()
                .filter(h -> h != null && "P/E".equalsIgnoreCase(h.metric()))
                .findFirst();
    }
}
