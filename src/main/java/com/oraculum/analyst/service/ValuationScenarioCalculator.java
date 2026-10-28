package com.oraculum.analyst.service;

import com.oraculum.analyst.dto.ValuationScenarioDto;
import com.oraculum.analyst.dto.ValuationScenarioDto.ReverseDcfSummary;
import com.oraculum.analyst.dto.ValuationScenarioDto.SensitivityMatrix;
import com.oraculum.analyst.dto.ValuationScenarioInputDto;
import com.oraculum.analyst.service.valuation.EpsReconciliationCalculator;
import com.oraculum.analyst.service.valuation.EpsReconciliationCalculator.EpsReconciliationResult;
import com.oraculum.analyst.service.valuation.SensitivityMatrixCalculator;
import com.oraculum.analyst.service.valuation.ValuationDivergenceEvaluator;
import com.oraculum.analyst.service.valuation.ValuationDivergenceEvaluator.MethodDivergenceResult;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.company.api.dto.HistoricalValuationSummaryDto;
import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
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

    public ValuationScenarioCalculator() {
        this(new EpsReconciliationCalculator(), new SensitivityMatrixCalculator(), new ValuationDivergenceEvaluator());
    }

    private static Float round2(double val) {
        return (float) (Math.round(val * 100.0) / 100.0);
    }

    public ValuationScenarioDto calculate(ValuationScenarioInputDto input) {
        if (input == null || input.estimates() == null || input.estimates().isEmpty()
                || input.currentSharePrice() == null || input.currentSharePrice() <= 0) {
            return ValuationScenarioDto.empty();
        }
        return executeCalculation(input);
    }

    public ValuationScenarioDto calculate(
            Float currentSharePrice,
            List<EarningsEstimateDto> estimates,
            LocalDate analysisDate,
            ReverseDcfDto reverseDcfResult
    ) {
        return calculate(ValuationScenarioInputDto.builder()
                .currentSharePrice(currentSharePrice)
                .estimates(estimates)
                .analysisDate(analysisDate)
                .reverseDcfResult(reverseDcfResult)
                .build());
    }

    public ValuationScenarioDto calculate(
            Float currentSharePrice,
            List<EarningsEstimateDto> estimates,
            LocalDate analysisDate,
            ReverseDcfDto reverseDcfResult,
            List<Float> referenceMultiples
    ) {
        return calculate(ValuationScenarioInputDto.builder()
                .currentSharePrice(currentSharePrice)
                .estimates(estimates)
                .analysisDate(analysisDate)
                .reverseDcfResult(reverseDcfResult)
                .explicitMultiples(referenceMultiples)
                .build());
    }

    public ValuationScenarioDto calculate(
            Float currentSharePrice,
            List<EarningsEstimateDto> estimates,
            LocalDate analysisDate,
            ReverseDcfDto reverseDcfResult,
            List<CompanyFinancialRatiosDto> quarterlyRatios,
            List<HistoricalValuationSummaryDto> historicalValuation
    ) {
        return calculate(ValuationScenarioInputDto.builder()
                .currentSharePrice(currentSharePrice)
                .estimates(estimates)
                .analysisDate(analysisDate)
                .reverseDcfResult(reverseDcfResult)
                .quarterlyRatios(quarterlyRatios)
                .historicalValuation(historicalValuation)
                .build());
    }

    private ValuationScenarioDto executeCalculation(ValuationScenarioInputDto input) {
        List<EarningsEstimateDto> fyEstimates = filterFiscalYears(input.estimates(), input.analysisDate());
        List<EarningsEstimateDto> qEstimates = filterFiscalQuarters(input.estimates(), input.analysisDate());
        EarningsEstimateDto fy1 = !fyEstimates.isEmpty() ? fyEstimates.getFirst() : null;
        EarningsEstimateDto fy2 = fyEstimates.size() > 1 ? fyEstimates.get(1) : null;

        Float forwardPeFy1 = calculateForwardPe(input.currentSharePrice(), fy1);
        Float forwardPeFy2 = calculateForwardPe(input.currentSharePrice(), fy2);
        EarningsEstimateDto nextQ = !qEstimates.isEmpty() ? qEstimates.getFirst() : null;

        EpsReconciliationResult reconciliation = epsReconciliationCalculator.reconcile(
                fy1, qEstimates, input.quarterlyRatios()
        );
        SensitivityMatrix matrix = sensitivityMatrixCalculator.calculate(
                input.currentSharePrice(), fy2 != null ? fy2 : fy1, input.explicitMultiples(), input.historicalValuation()
        );
        ReverseDcfSummary dcfSummary = valuationDivergenceEvaluator.buildReverseDcfSummary(input.reverseDcfResult());
        MethodDivergenceResult divergence = valuationDivergenceEvaluator.evaluateMethodDivergence(
                input.reverseDcfResult(), forwardPeFy1, input.historicalValuation()
        );

        return buildDto(input.currentSharePrice(), forwardPeFy1, forwardPeFy2, nextQ, reconciliation, matrix, dcfSummary, divergence);
    }

    private ValuationScenarioDto buildDto(
            Float currentPrice,
            Float peFy1,
            Float peFy2,
            EarningsEstimateDto nextQ,
            EpsReconciliationResult reconciliation,
            SensitivityMatrix matrix,
            ReverseDcfSummary dcfSummary,
            MethodDivergenceResult divergence
    ) {
        return new ValuationScenarioDto(
                currentPrice,
                peFy1,
                peFy2,
                nextQ != null ? nextQ.date() : null,
                extractFloat(nextQ != null ? nextQ.epsEstimateAverage() : null),
                extractFloat(nextQ != null ? nextQ.revenueEstimateAverage() : null),
                reconciliation.divergence(),
                reconciliation.status(),
                reconciliation.note(),
                matrix,
                dcfSummary,
                divergence.divergent(),
                divergence.note()
        );
    }

    private Float extractFloat(BigDecimal value) {
        return value != null ? value.floatValue() : null;
    }

    private Float calculateForwardPe(Float price, EarningsEstimateDto estimate) {
        if (estimate == null || estimate.epsEstimateAverage() == null) {
            return null;
        }
        double eps = estimate.epsEstimateAverage().doubleValue();
        return eps > 0.001 ? round2(price / eps) : null;
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
        String h = dto.horizon().trim().toLowerCase(Locale.ROOT);
        return h.contains("year") || h.contains("annual");
    }

    private boolean isFiscalQuarter(EarningsEstimateDto dto) {
        if (dto.horizon() == null) return false;
        return dto.horizon().trim().toLowerCase(Locale.ROOT).contains("quarter");
    }
}
