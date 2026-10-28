package com.oraculum.analyst.service.valuation;

import com.oraculum.analyst.dto.ValuationScenarioDto.*;
import com.oraculum.company.api.dto.HistoricalValuationSummaryDto;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class SensitivityMatrixCalculator {

    public static final List<Float> DEFAULT_REFERENCE_MULTIPLES = List.of(12.0f, 16.0f, 20.0f);
    public static final String SENSITIVITY_DISCLAIMER =
            "Illustrative sensitivity calculation only, not a price forecast or target.";

    private static Float round2(double val) {
        return (float) (Math.round(val * 100.0) / 100.0);
    }

    private static Float round1(double val) {
        return (float) (Math.round(val * 10.0) / 10.0);
    }

    public SensitivityMatrix calculate(
            Float currentPrice,
            EarningsEstimateDto target,
            List<Float> explicitMultiples,
            List<HistoricalValuationSummaryDto> historicalValuation
    ) {
        if (target == null) {
            return null;
        }
        List<MultipleAnchor> anchors = resolveMultipleAnchors(explicitMultiples, historicalValuation);
        Float epsLow = extractFloat(target.epsEstimateLow());
        Float epsAvg = extractFloat(target.epsEstimateAverage());
        Float epsHigh = extractFloat(target.epsEstimateHigh());

        List<Float> refs = anchors.stream().map(MultipleAnchor::multiple).toList();
        List<SensitivityCell> cells = buildMatrixCells(currentPrice, epsLow, epsAvg, epsHigh, refs);
        List<ImpliedEpsEntry> impliedEps = buildImpliedEpsEntries(currentPrice, epsAvg, anchors);
        MatrixSummary summary = buildMatrixSummary(currentPrice, epsAvg, refs, cells);
        String targetLabel = target.date() != null ? target.date().toString() : "Next FY";

        return new SensitivityMatrix(
                targetLabel, epsLow, epsAvg, epsHigh, refs, anchors, impliedEps, summary, cells, SENSITIVITY_DISCLAIMER
        );
    }

    public List<MultipleAnchor> resolveMultipleAnchors(
            List<Float> explicitMultiples,
            List<HistoricalValuationSummaryDto> historicalValuation
    ) {
        if (explicitMultiples != null && !explicitMultiples.isEmpty()) {
            return buildExplicitAnchors(explicitMultiples);
        }
        Optional<HistoricalValuationSummaryDto> peOpt = findPeHistoricalSummary(historicalValuation);
        if (peOpt.isPresent() && peOpt.get().min10y() != null && peOpt.get().min10y() > 5.0f) {
            return buildHistoricalPeAnchors(peOpt.get());
        }
        return List.of(
                new MultipleAnchor(12.0f, "Conservative Multiple (12.0x)"),
                new MultipleAnchor(16.0f, "Baseline Market Multiple (16.0x)"),
                new MultipleAnchor(20.0f, "Growth Multiple (20.0x)")
        );
    }

    private List<MultipleAnchor> buildExplicitAnchors(List<Float> multiples) {
        List<MultipleAnchor> anchors = new ArrayList<>();
        for (Float m : multiples) {
            if (m != null) {
                anchors.add(new MultipleAnchor(m, String.format(Locale.ROOT, "Reference Multiple (%.1fx)", m)));
            }
        }
        return anchors;
    }

    private Optional<HistoricalValuationSummaryDto> findPeHistoricalSummary(List<HistoricalValuationSummaryDto> list) {
        if (list == null) return Optional.empty();
        return list.stream()
                .filter(h -> h != null && "P/E".equalsIgnoreCase(h.metric()))
                .findFirst();
    }

    private List<MultipleAnchor> buildHistoricalPeAnchors(HistoricalValuationSummaryDto pe) {
        float min10y = pe.min10y();
        if (min10y > 15.0f) {
            float low = round1(Math.max(12.0f, min10y * 0.75f));
            float mid = round1(Math.min(min10y, 25.0f));
            float high = round1(resolveUpperMultiple(pe, mid));
            return List.of(
                    new MultipleAnchor(low, String.format(Locale.ROOT, "Cyclical Compression (%.1fx, 0.75x 10Y Min)", low)),
                    new MultipleAnchor(mid, String.format(Locale.ROOT, "10Y Historical Min P/E (%.1fx)", mid)),
                    new MultipleAnchor(high, String.format(Locale.ROOT, "Current / Historical Anchor P/E (%.1fx)", high))
            );
        }
        float low = round1(Math.max(8.0f, min10y));
        float mid = round1(resolveMidMultiple(pe, low));
        float high = round1(resolveUpperMultiple(pe, mid));
        return List.of(
                new MultipleAnchor(low, String.format(Locale.ROOT, "10Y Historical Min P/E (%.1fx)", low)),
                new MultipleAnchor(mid, String.format(Locale.ROOT, "Historical Baseline P/E (%.1fx)", mid)),
                new MultipleAnchor(high, String.format(Locale.ROOT, "Upper Valuation Anchor P/E (%.1fx)", high))
        );
    }

    private float resolveMidMultiple(HistoricalValuationSummaryDto pe, float low) {
        float candidate = pe.avg10y() != null && pe.avg10y() > low ? pe.avg10y() : low * 1.35f;
        return Math.clamp(candidate, low + 3.0f, 24.0f);
    }

    private float resolveUpperMultiple(HistoricalValuationSummaryDto pe, float mid) {
        float candidate = pe.current() != null && pe.current() > mid ? pe.current() : mid * 1.3f;
        return Math.clamp(candidate, mid + 3.0f, 32.0f);
    }

    private List<SensitivityCell> buildMatrixCells(
            Float currentPrice,
            Float epsLow,
            Float epsAvg,
            Float epsHigh,
            List<Float> refs
    ) {
        List<SensitivityCell> cells = new ArrayList<>();
        for (Float multiple : refs) {
            addCellIfPresent(cells, multiple, "LOW", epsLow, currentPrice);
            addCellIfPresent(cells, multiple, "AVG", epsAvg, currentPrice);
            addCellIfPresent(cells, multiple, "HIGH", epsHigh, currentPrice);
        }
        return cells;
    }

    private void addCellIfPresent(List<SensitivityCell> cells, Float multiple, String scenario, Float eps, Float currentPrice) {
        if (multiple == null || eps == null || currentPrice == null || currentPrice <= 0) {
            return;
        }
        float implied = round2(multiple * eps);
        float pctMove = round1(((implied - currentPrice) / currentPrice) * 100.0);
        cells.add(new SensitivityCell(multiple, scenario, eps, implied, pctMove));
    }

    private List<ImpliedEpsEntry> buildImpliedEpsEntries(Float currentPrice, Float epsAvg, List<MultipleAnchor> anchors) {
        if (currentPrice == null || currentPrice <= 0 || anchors == null) {
            return List.of();
        }
        return anchors.stream()
                .filter(a -> a.multiple() != null && a.multiple() > 0)
                .map(a -> createImpliedEpsEntry(currentPrice, epsAvg, a))
                .toList();
    }

    private ImpliedEpsEntry createImpliedEpsEntry(Float currentPrice, Float epsAvg, MultipleAnchor anchor) {
        float reqEps = round2(currentPrice / anchor.multiple());
        Float diffPct = (epsAvg != null && reqEps > 0)
                ? round1(((epsAvg - reqEps) / reqEps) * 100.0)
                : null;
        return new ImpliedEpsEntry(anchor.multiple(), anchor.label(), reqEps, epsAvg, diffPct);
    }

    private MatrixSummary buildMatrixSummary(
            Float currentPrice,
            Float epsAvg,
            List<Float> refs,
            List<SensitivityCell> cells
    ) {
        if (cells.isEmpty()) {
            return null;
        }
        List<Float> moves = cells.stream()
                .map(SensitivityCell::pctMoveVsCurrent)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        Float minMove = moves.isEmpty() ? null : moves.getFirst();
        Float maxMove = moves.isEmpty() ? null : moves.getLast();
        Float medianMove = calculateMedian(moves);
        Float baseMultiple = refs.isEmpty() ? null : refs.get(refs.size() / 2);
        SensitivityCell baseCell = findBaseCell(cells, baseMultiple);
        Float breakEvenPe = (epsAvg != null && epsAvg > 0.001f && currentPrice != null)
                ? round2(currentPrice / epsAvg)
                : null;

        return new MatrixSummary(
                minMove, maxMove, medianMove, baseMultiple,
                baseCell != null ? baseCell.impliedPrice() : null,
                baseCell != null ? baseCell.pctMoveVsCurrent() : null,
                breakEvenPe
        );
    }

    private SensitivityCell findBaseCell(List<SensitivityCell> cells, Float baseMultiple) {
        if (baseMultiple == null) return null;
        return cells.stream()
                .filter(c -> baseMultiple.equals(c.multiple()) && "AVG".equals(c.epsScenario()))
                .findFirst()
                .orElse(null);
    }

    private Float calculateMedian(List<Float> sorted) {
        if (sorted == null || sorted.isEmpty()) return null;
        int mid = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(mid);
        }
        return round1((sorted.get(mid - 1) + sorted.get(mid)) / 2.0);
    }

    private Float extractFloat(BigDecimal value) {
        return value != null ? value.floatValue() : null;
    }
}
