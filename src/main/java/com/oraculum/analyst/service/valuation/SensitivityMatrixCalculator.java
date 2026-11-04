package com.oraculum.analyst.service.valuation;

import com.oraculum.analyst.dto.valuation.ImpliedEpsEntryDto;
import com.oraculum.analyst.dto.valuation.MatrixSummaryDto;
import com.oraculum.analyst.dto.valuation.MultipleAnchorDto;
import com.oraculum.analyst.dto.valuation.SensitivityCellDto;
import com.oraculum.analyst.dto.valuation.SensitivityMatrixDto;
import com.oraculum.company.api.dto.HistoricalValuationSummaryDto;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import com.oraculum.util.NumericUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Component
public class SensitivityMatrixCalculator {

    public static final List<Float> DEFAULT_REFERENCE_MULTIPLES = List.of(12.0f, 16.0f, 20.0f);
    public static final String SENSITIVITY_DISCLAIMER =
            "Illustrative sensitivity calculation only, not a price forecast or target.";

    public SensitivityMatrixDto calculate(
            Float currentPrice,
            EarningsEstimateDto target,
            List<Float> explicitMultiples,
            List<HistoricalValuationSummaryDto> historicalValuation
    ) {
        return calculate(currentPrice, target, explicitMultiples, historicalValuation, null);
    }

    public SensitivityMatrixDto calculate(
            Float currentPrice,
            EarningsEstimateDto target,
            List<Float> explicitMultiples,
            List<HistoricalValuationSummaryDto> historicalValuation,
            LocalDate analysisDate
    ) {
        if (target == null) {
            return null;
        }
        List<MultipleAnchorDto> anchors = resolveMultipleAnchors(explicitMultiples, historicalValuation);
        Float epsLow = extractFloat(target.epsEstimateLow());
        Float epsAvg = extractFloat(target.epsEstimateAverage());
        Float epsHigh = extractFloat(target.epsEstimateHigh());

        double discountFactor = computeDiscountFactor(analysisDate, target.date());
        List<Float> refs = anchors.stream().map(MultipleAnchorDto::multiple).toList();
        List<SensitivityCellDto> cells = buildMatrixCells(currentPrice, epsLow, epsAvg, epsHigh, refs, discountFactor);

        return SensitivityMatrixDto.builder()
                .targetFiscalYear(target.date() != null ? target.date().toString() : "Next FY")
                .epsLow(epsLow)
                .epsAvg(epsAvg)
                .epsHigh(epsHigh)
                .referenceMultiples(refs)
                .multipleAnchors(anchors)
                .impliedEpsByMultiple(buildImpliedEpsEntries(currentPrice, epsAvg, anchors))
                .matrixSummary(buildMatrixSummary(currentPrice, epsAvg, refs, cells))
                .cells(cells)
                .disclaimer(SENSITIVITY_DISCLAIMER)
                .build();
    }

    private double computeDiscountFactor(LocalDate analysisDate, LocalDate targetDate) {
        if (analysisDate == null || targetDate == null || targetDate.isBefore(analysisDate)) {
            return 1.0;
        }
        long days = ChronoUnit.DAYS.between(analysisDate, targetDate);
        double years = Math.max(0.1, days / 365.25);
        return Math.pow(1.10, years);
    }

    public List<MultipleAnchorDto> resolveMultipleAnchors(
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
                new MultipleAnchorDto(12.0f, "Conservative Multiple (12.0x)"),
                new MultipleAnchorDto(16.0f, "Baseline Market Multiple (16.0x)"),
                new MultipleAnchorDto(20.0f, "Growth Multiple (20.0x)")
        );
    }

    private List<MultipleAnchorDto> buildExplicitAnchors(List<Float> multiples) {
        List<MultipleAnchorDto> anchors = new ArrayList<>();
        for (Float m : multiples) {
            if (m != null) {
                anchors.add(new MultipleAnchorDto(m, String.format(Locale.ROOT, "Reference Multiple (%.1fx)", m)));
            }
        }
        return anchors;
    }

    private Optional<HistoricalValuationSummaryDto> findPeHistoricalSummary(List<HistoricalValuationSummaryDto> list) {
        if (list == null) {
            return Optional.empty();
        }
        return list.stream()
                .filter(h -> h != null && "P/E".equalsIgnoreCase(h.metric()))
                .findFirst();
    }

    private List<MultipleAnchorDto> buildHistoricalPeAnchors(HistoricalValuationSummaryDto pe) {
        float min10y = pe.min10y();
        if (min10y > 15.0f) {
            float low = NumericUtil.round1(Math.max(12.0f, min10y * 0.75f));
            float mid = NumericUtil.round1(Math.min(min10y, 25.0f));
            float high = NumericUtil.round1(resolveUpperMultiple(pe, mid));
            return List.of(
                    new MultipleAnchorDto(low, String.format(Locale.ROOT, "Cyclical Compression (%.1fx, 0.75x 10Y Min)", low)),
                    new MultipleAnchorDto(mid, String.format(Locale.ROOT, "10Y Historical Min P/E (%.1fx)", mid)),
                    new MultipleAnchorDto(high, String.format(Locale.ROOT, "Current / Historical Anchor P/E (%.1fx)", high))
            );
        }
        float low = NumericUtil.round1(Math.max(8.0f, min10y));
        float mid = NumericUtil.round1(resolveMidMultiple(pe, low));
        float high = NumericUtil.round1(resolveUpperMultiple(pe, mid));
        return List.of(
                new MultipleAnchorDto(low, String.format(Locale.ROOT, "10Y Historical Min P/E (%.1fx)", low)),
                new MultipleAnchorDto(mid, String.format(Locale.ROOT, "Historical Baseline P/E (%.1fx)", mid)),
                new MultipleAnchorDto(high, String.format(Locale.ROOT, "Upper Valuation Anchor P/E (%.1fx)", high))
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

    private List<SensitivityCellDto> buildMatrixCells(
            Float currentPrice,
            Float epsLow,
            Float epsAvg,
            Float epsHigh,
            List<Float> refs,
            double discountFactor
    ) {
        List<SensitivityCellDto> cells = new ArrayList<>();
        for (Float multiple : refs) {
            addCellIfPresent(cells, multiple, "LOW", epsLow, currentPrice, discountFactor);
            addCellIfPresent(cells, multiple, "AVG", epsAvg, currentPrice, discountFactor);
            addCellIfPresent(cells, multiple, "HIGH", epsHigh, currentPrice, discountFactor);
        }
        return cells;
    }

    private void addCellIfPresent(
            List<SensitivityCellDto> cells,
            Float multiple,
            String scenario,
            Float eps,
            Float currentPrice,
            double discountFactor
    ) {
        if (multiple == null || eps == null || currentPrice == null || currentPrice <= 0) {
            return;
        }
        Float implied = NumericUtil.round2(multiple * eps);
        Float pctMove = NumericUtil.round1(((implied - currentPrice) / currentPrice) * 100.0);
        Float discounted = NumericUtil.round2(implied / discountFactor);
        Float discPctMove = NumericUtil.round1(((discounted - currentPrice) / currentPrice) * 100.0);

        cells.add(SensitivityCellDto.builder()
                .multiple(multiple)
                .epsScenario(scenario)
                .epsValue(eps)
                .impliedPrice(implied)
                .pctMoveVsCurrent(pctMove)
                .discountedImpliedPrice(discounted)
                .discountedPctMoveVsCurrent(discPctMove)
                .build());
    }

    private List<ImpliedEpsEntryDto> buildImpliedEpsEntries(
            Float currentPrice,
            Float epsAvg,
            List<MultipleAnchorDto> anchors
    ) {
        if (currentPrice == null || currentPrice <= 0 || anchors == null) {
            return List.of();
        }
        return anchors.stream()
                .filter(a -> a.multiple() != null && a.multiple() > 0)
                .map(a -> createImpliedEpsEntry(currentPrice, epsAvg, a))
                .toList();
    }

    private ImpliedEpsEntryDto createImpliedEpsEntry(Float currentPrice, Float epsAvg, MultipleAnchorDto anchor) {
        Float reqEps = NumericUtil.round2(currentPrice / anchor.multiple());
        Float diffPct = (epsAvg != null && reqEps > 0)
                ? NumericUtil.round1(((epsAvg - reqEps) / reqEps) * 100.0)
                : null;
        return ImpliedEpsEntryDto.builder()
                .multiple(anchor.multiple())
                .anchorLabel(anchor.label())
                .requiredEpsForCurrentPrice(reqEps)
                .consensusEpsAvg(epsAvg)
                .consensusVsRequiredPct(diffPct)
                .build();
    }

    private MatrixSummaryDto buildMatrixSummary(
            Float currentPrice,
            Float epsAvg,
            List<Float> refs,
            List<SensitivityCellDto> cells
    ) {
        if (cells.isEmpty()) {
            return null;
        }
        List<Float> moves = extractSortedMoves(cells, false);
        List<Float> discMoves = extractSortedMoves(cells, true);
        Float baseMultiple = refs.isEmpty() ? null : refs.get(refs.size() / 2);
        SensitivityCellDto baseCell = findBaseCell(cells, baseMultiple);
        Float breakEvenPe = (epsAvg != null && epsAvg > 0.001f && currentPrice != null)
                ? NumericUtil.round2(currentPrice / epsAvg)
                : null;

        return MatrixSummaryDto.builder()
                .minPctMove(moves.isEmpty() ? null : moves.getFirst())
                .maxPctMove(moves.isEmpty() ? null : moves.getLast())
                .medianPctMove(NumericUtil.medianFloat(moves))
                .baseCaseMultiple(baseMultiple)
                .baseCaseImpliedPrice(baseCell != null ? baseCell.impliedPrice() : null)
                .baseCasePctMove(baseCell != null ? baseCell.pctMoveVsCurrent() : null)
                .discountedMinPctMove(discMoves.isEmpty() ? null : discMoves.getFirst())
                .discountedMaxPctMove(discMoves.isEmpty() ? null : discMoves.getLast())
                .discountedMedianPctMove(NumericUtil.medianFloat(discMoves))
                .discountedBaseCaseImpliedPrice(baseCell != null ? baseCell.discountedImpliedPrice() : null)
                .discountedBaseCasePctMove(baseCell != null ? baseCell.discountedPctMoveVsCurrent() : null)
                .breakEvenPeOnAvgEps(breakEvenPe)
                .build();
    }

    private List<Float> extractSortedMoves(List<SensitivityCellDto> cells, boolean discounted) {
        return cells.stream()
                .map(c -> discounted ? c.discountedPctMoveVsCurrent() : c.pctMoveVsCurrent())
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }

    private SensitivityCellDto findBaseCell(List<SensitivityCellDto> cells, Float baseMultiple) {
        if (baseMultiple == null) {
            return null;
        }
        return cells.stream()
                .filter(c -> baseMultiple.equals(c.multiple()) && "AVG".equals(c.epsScenario()))
                .findFirst()
                .orElse(null);
    }

    private Float extractFloat(BigDecimal value) {
        return value != null ? value.floatValue() : null;
    }
}
