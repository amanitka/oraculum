package com.oraculum.analyst.service.valuation;

import com.oraculum.analyst.service.valuation.EpsReconciliationCalculator.EpsReconciliationResult;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EpsReconciliationCalculatorTest {

    private EpsReconciliationCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new EpsReconciliationCalculator();
    }

    @Test
    void reconcile_nullOrZeroFy_returnsDefaultResult() {
        EpsReconciliationResult resultNull = calculator.reconcile(null, List.of(), List.of());
        EarningsEstimateDto zeroFy = createEstimate("annual", LocalDate.of(2026, 12, 31), 0.0);
        EpsReconciliationResult resultZero = calculator.reconcile(zeroFy, List.of(), List.of());

        assertThat(resultNull.divergence()).isFalse();
        assertThat(resultNull.status()).isNull();
        assertThat(resultZero.divergence()).isFalse();
    }

    @Test
    void reconcile_incompleteQuarters_returnsIncompleteStatus() {
        EarningsEstimateDto fy = createEstimate("annual", LocalDate.of(2027, 1, 31), 9.31);
        List<EarningsEstimateDto> futureQuarters = List.of(
                createEstimate("quarter", LocalDate.of(2026, 10, 31), 2.47)
        );

        EpsReconciliationResult result = calculator.reconcile(fy, futureQuarters, List.of());

        assertThat(result.divergence()).isFalse();
        assertThat(result.status()).isEqualTo("INCOMPLETE_QUARTERS");
        assertThat(result.note()).contains("Only 1 of 4 quarters available");
    }

    @Test
    void reconcile_allEstimatedQuarters_alignedWhenWithinEightPercent() {
        EarningsEstimateDto fy = createEstimate("annual", LocalDate.of(2026, 12, 31), 10.0);
        List<EarningsEstimateDto> quarters = List.of(
                createEstimate("quarter", LocalDate.of(2026, 3, 31), 2.45),
                createEstimate("quarter", LocalDate.of(2026, 6, 30), 2.50),
                createEstimate("quarter", LocalDate.of(2026, 9, 30), 2.55),
                createEstimate("quarter", LocalDate.of(2026, 12, 31), 2.60)
        ); // sum = 10.10 (+1.0% diff)

        EpsReconciliationResult result = calculator.reconcile(fy, quarters, List.of());

        assertThat(result.divergence()).isFalse();
        assertThat(result.status()).isEqualTo("ALIGNED");
        assertThat(result.note()).isNull();
    }

    @Test
    void reconcile_combinesActualsAndEstimates_classifiesGaapBasisDifference() {
        EarningsEstimateDto fy = createEstimate("fiscal year", LocalDate.of(2027, 1, 31), 9.31);
        List<EarningsEstimateDto> estimates = List.of(
                createEstimate("fiscal quarter", LocalDate.of(2026, 10, 31), 2.47),
                createEstimate("fiscal quarter", LocalDate.of(2027, 1, 31), 2.75)
        );
        List<CompanyFinancialRatiosDto> actuals = List.of(
                createQuarterlyRatio(LocalDate.of(2026, 4, 30), 1.59f),
                createQuarterlyRatio(LocalDate.of(2026, 7, 31), 1.65f)
        ); // combined = 8.46 vs 9.31 -> 9.1% diff (<= 15%)

        EpsReconciliationResult result = calculator.reconcile(fy, estimates, actuals);

        assertThat(result.divergence()).isFalse();
        assertThat(result.status()).isEqualTo("GAAP_NON_GAAP_BASIS_DIFF");
        assertThat(result.note())
                .contains("Combined 2Q GAAP actuals")
                .contains("2Q consensus estimates")
                .contains("forward P/E is reliable");
    }

    @Test
    void reconcile_materialDivergence_flagsDivergenceTrue() {
        EarningsEstimateDto fy = createEstimate("annual", LocalDate.of(2026, 12, 31), 10.0);
        List<EarningsEstimateDto> quarters = List.of(
                createEstimate("quarter", LocalDate.of(2026, 3, 31), 1.8),
                createEstimate("quarter", LocalDate.of(2026, 6, 30), 1.8),
                createEstimate("quarter", LocalDate.of(2026, 9, 30), 1.8),
                createEstimate("quarter", LocalDate.of(2026, 12, 31), 1.8)
        ); // sum = 7.2 vs 10.0 -> 28% diff (> 15%)

        EpsReconciliationResult result = calculator.reconcile(fy, quarters, List.of());

        assertThat(result.divergence()).isTrue();
        assertThat(result.status()).isEqualTo("MATERIAL_DIVERGENCE");
        assertThat(result.note()).contains("deviates by 28.0%");
    }

    private EarningsEstimateDto createEstimate(String horizon, LocalDate date, double avg) {
        return new EarningsEstimateDto(
                date, horizon, BigDecimal.valueOf(avg), null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null
        );
    }

    private CompanyFinancialRatiosDto createQuarterlyRatio(LocalDate reportDate, float eps) {
        return new CompanyFinancialRatiosDto(
                "id-" + reportDate, 1, "NVDA", "USD", null, StatementVariant.QUARTERLY,
                reportDate.getYear(), "Q", reportDate, reportDate, reportDate,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, eps, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null,
                null, null, null, null, null, 0, 0, 0
        );
    }
}
