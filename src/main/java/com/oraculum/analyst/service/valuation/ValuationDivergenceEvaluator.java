package com.oraculum.analyst.service.valuation;

import com.oraculum.analyst.dto.ValuationScenarioDto.ReverseDcfSummary;
import com.oraculum.company.api.dto.HistoricalValuationSummaryDto;
import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.company.api.dto.ReverseDcfMatrixCellDto;
import com.oraculum.company.api.dto.ValuationZone;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class ValuationDivergenceEvaluator {

    public record MethodDivergenceResult(boolean divergent, String note) {}

    public ReverseDcfSummary buildReverseDcfSummary(ReverseDcfDto dcf) {
        if (dcf == null) return null;
        List<ReverseDcfMatrixCellDto> cells = (dcf.sensitivityMatrix() != null && dcf.sensitivityMatrix().cells() != null)
                ? dcf.sensitivityMatrix().cells()
                : List.of();
        ReverseDcfMatrixCellDto baseCell = cells.stream().filter(ReverseDcfMatrixCellDto::isBaseCase).findFirst().orElse(null);
        List<Float> impliedRates = cells.stream()
                .map(c -> round2(c.impliedFcfGrowthPct()))
                .sorted()
                .toList();
        Integer aggPct = calculateAggressiveCellsPct(cells);
        String zone = baseCell != null && baseCell.valuationZone() != null ? baseCell.valuationZone().name() : null;
        Float baseGrowth = dcf.impliedFcfGrowthRatePct() != null ? round2(dcf.impliedFcfGrowthRatePct()) : null;
        Float histCagr = dcf.historicalFcfCagrPct() != null ? round2(dcf.historicalFcfCagrPct()) : null;

        return new ReverseDcfSummary(
                baseGrowth,
                zone,
                impliedRates.isEmpty() ? null : impliedRates.getFirst(),
                impliedRates.isEmpty() ? null : impliedRates.getLast(),
                calculateMedian(impliedRates),
                aggPct,
                histCagr
        );
    }

    public MethodDivergenceResult evaluateMethodDivergence(
            ReverseDcfDto reverseDcf,
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
        String note = String.format(Locale.ROOT,
                "METHOD CONFLICT: Multiples appear moderate/low (FY1 Forward P/E %s, 10Y P/E percentile %s), while Reverse DCF is in %s zone requiring %.1f%% 10Y FCF CAGR. Assess peak-cycle margin compression risk.",
                forwardPe != null ? String.format(Locale.ROOT, "%.1fx", forwardPe) : "N/A",
                pePercentile != null ? pePercentile + "%" : "N/A",
                zoneName,
                impliedGrowth);
        return new MethodDivergenceResult(true, note);
    }

    private Integer calculateAggressiveCellsPct(List<ReverseDcfMatrixCellDto> cells) {
        if (cells.isEmpty()) return null;
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
        if (list == null) return Optional.empty();
        return list.stream()
                .filter(h -> h != null && "P/E".equalsIgnoreCase(h.metric()))
                .findFirst();
    }

    private Float calculateMedian(List<Float> sorted) {
        if (sorted == null || sorted.isEmpty()) return null;
        int mid = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(mid);
        }
        return round1((sorted.get(mid - 1) + sorted.get(mid)) / 2.0);
    }

    private static Float round2(double val) {
        return (float) (Math.round(val * 100.0) / 100.0);
    }

    private static Float round1(double val) {
        return (float) (Math.round(val * 10.0) / 10.0);
    }
}
