package com.oraculum.analyst.service.valuation;

import com.oraculum.analyst.dto.valuation.EpsReconciliationResult;
import com.oraculum.analyst.dto.valuation.ForwardReverseDcfDto;
import com.oraculum.analyst.dto.valuation.ForwardReverseDcfRequest;
import com.oraculum.analyst.dto.valuation.MethodDivergenceResult;
import com.oraculum.analyst.dto.valuation.SensitivityMatrixDto;
import com.oraculum.analyst.dto.valuation.ValuationScenarioDto;
import com.oraculum.analyst.dto.valuation.ValuationScenarioRequest;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import com.oraculum.util.NumericUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ValuationScenarioCalculator {

    public static final List<Float> DEFAULT_REFERENCE_MULTIPLES = SensitivityMatrixCalculator.DEFAULT_REFERENCE_MULTIPLES;

    private final EpsReconciliationCalculator epsReconciliationCalculator;
    private final SensitivityMatrixCalculator sensitivityMatrixCalculator;
    private final ValuationDivergenceEvaluator valuationDivergenceEvaluator;
    private final ForwardReverseDcfCalculator forwardReverseDcfCalculator;
    private final OperatingEarningsCalculator operatingEarningsCalculator;

    public ValuationScenarioCalculator() {
        this(
                new EpsReconciliationCalculator(),
                new SensitivityMatrixCalculator(),
                new ValuationDivergenceEvaluator(),
                new ForwardReverseDcfCalculator(),
                new OperatingEarningsCalculator()
        );
    }

    public ValuationScenarioDto calculate(ValuationScenarioRequest request) {
        if (request == null || request.estimates().isEmpty()
                || request.currentSharePrice() == null || request.currentSharePrice() <= 0) {
            return ValuationScenarioDto.empty();
        }
        return executeCalculation(request);
    }

    private ValuationScenarioDto executeCalculation(ValuationScenarioRequest request) {
        List<EarningsEstimateDto> fyEstimates = filterFiscalYears(request.estimates(), request.analysisDate());
        List<EarningsEstimateDto> qEstimates = filterFiscalQuarters(request.estimates(), request.analysisDate());
        EarningsEstimateDto fy1 = !fyEstimates.isEmpty() ? fyEstimates.getFirst() : null;
        EarningsEstimateDto fy2 = fyEstimates.size() > 1 ? fyEstimates.get(1) : null;
        EarningsEstimateDto nextQ = !qEstimates.isEmpty() ? qEstimates.getFirst() : null;

        Float forwardPeFy1 = calculateForwardPe(request.currentSharePrice(), fy1);
        Float forwardPeFy2 = calculateForwardPe(request.currentSharePrice(), fy2);
        EpsReconciliationResult reconciliation = epsReconciliationCalculator.reconcile(
                fy1, qEstimates, request.quarterlyRatios()
        );
        SensitivityMatrixDto matrix = sensitivityMatrixCalculator.calculate(
                request.currentSharePrice(), fy2 != null ? fy2 : fy1,
                request.explicitMultiples(), request.historicalValuation(), request.analysisDate()
        );
        ForwardReverseDcfDto twoStageDcf = computeTwoStageDcf(request, fy1, fy2);
        MethodDivergenceResult divergence = valuationDivergenceEvaluator.evaluateMethodDivergence(
                request.reverseDcfResult(), twoStageDcf, forwardPeFy1, request.historicalValuation()
        );

        ValuationScenarioDto.ValuationScenarioDtoBuilder builder = ValuationScenarioDto.builder()
                .currentSharePrice(request.currentSharePrice())
                .forwardPeFy1(forwardPeFy1)
                .forwardPeFy2(forwardPeFy2)
                .sensitivityMatrix(matrix)
                .reverseDcfSummary(valuationDivergenceEvaluator.buildReverseDcfSummary(request.reverseDcfResult()))
                .twoStageReverseDcf(twoStageDcf)
                .operatingEarningsSummary(operatingEarningsCalculator.calculate(request.currentSharePrice(), request.ttmRatio()));

        applyNextQuarterAndFlags(builder, nextQ, reconciliation, divergence);
        return builder.build();
    }

    private void applyNextQuarterAndFlags(
            ValuationScenarioDto.ValuationScenarioDtoBuilder builder,
            EarningsEstimateDto nextQ,
            EpsReconciliationResult reconciliation,
            MethodDivergenceResult divergence
    ) {
        builder.nextQuarterEndDate(nextQ != null ? nextQ.date() : null)
                .nextQuarterEpsConsensus(extractFloat(nextQ != null ? nextQ.epsEstimateAverage() : null))
                .nextQuarterRevenueConsensus(extractFloat(nextQ != null ? nextQ.revenueEstimateAverage() : null))
                .flagEpsReconciliationDivergence(reconciliation.divergence())
                .epsReconciliationStatus(reconciliation.status())
                .epsReconciliationNote(reconciliation.note())
                .flagReverseDcfPeDivergence(divergence.divergent())
                .valuationMethodTensionNote(divergence.note());
    }

    private ForwardReverseDcfDto computeTwoStageDcf(
            ValuationScenarioRequest request,
            EarningsEstimateDto fy1,
            EarningsEstimateDto fy2
    ) {
        if (forwardReverseDcfCalculator == null) {
            return null;
        }
        Double marketCap = resolveMarketCap(request);
        if (marketCap == null || marketCap <= 0) {
            return null;
        }
        return forwardReverseDcfCalculator.calculate(ForwardReverseDcfRequest.builder()
                .marketCap(marketCap)
                .currentSharePrice(request.currentSharePrice())
                .analysisDate(request.analysisDate())
                .fy1(fy1)
                .fy2(fy2)
                .estimates(request.estimates())
                .annualRatios(request.annualRatios())
                .ttmRatio(request.ttmRatio())
                .build());
    }

    private Double resolveMarketCap(ValuationScenarioRequest request) {
        if (request.currentMarketCap() != null) {
            return request.currentMarketCap();
        }
        return request.reverseDcfResult() != null ? request.reverseDcfResult().currentMarketCap() : null;
    }

    private Float extractFloat(BigDecimal value) {
        return value != null ? value.floatValue() : null;
    }

    private Float calculateForwardPe(Float price, EarningsEstimateDto estimate) {
        if (estimate == null || estimate.epsEstimateAverage() == null) {
            return null;
        }
        double eps = estimate.epsEstimateAverage().doubleValue();
        return eps > 0.001 ? NumericUtil.round2(price / eps) : null;
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
        if (dto.horizon() == null) {
            return false;
        }
        String h = dto.horizon().trim().toLowerCase(Locale.ROOT);
        return h.contains("year") || h.contains("annual");
    }

    private boolean isFiscalQuarter(EarningsEstimateDto dto) {
        if (dto.horizon() == null) {
            return false;
        }
        return dto.horizon().trim().toLowerCase(Locale.ROOT).contains("quarter");
    }
}
