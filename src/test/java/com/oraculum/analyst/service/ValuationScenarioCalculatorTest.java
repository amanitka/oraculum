package com.oraculum.analyst.service;

import com.oraculum.analyst.dto.ValuationScenarioDto;
import com.oraculum.analyst.dto.ValuationScenarioDto.SensitivityCell;
import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ValuationScenarioCalculatorTest {

    private ValuationScenarioCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ValuationScenarioCalculator();
    }

    @Test
    void calculate_emptyEstimates_returnsEmptyDto() {
        ValuationScenarioDto result = calculator.calculate(150.0f, List.of(), LocalDate.of(2025, 1, 1), null);

        assertThat(result).isNotNull();
        assertThat(result.currentSharePrice()).isNull();
        assertThat(result.forwardPeFy1()).isNull();
    }

    @Test
    void calculate_nullOrZeroPrice_returnsEmptyDto() {
        List<EarningsEstimateDto> estimates = List.of(createEstimate("annual", LocalDate.of(2025, 12, 31), 5.0, 4.0, 6.0));

        ValuationScenarioDto resultNull = calculator.calculate(null, estimates, LocalDate.of(2025, 1, 1), null);
        ValuationScenarioDto resultZero = calculator.calculate(0.0f, estimates, LocalDate.of(2025, 1, 1), null);

        assertThat(resultNull.forwardPeFy1()).isNull();
        assertThat(resultZero.forwardPeFy1()).isNull();
    }

    @Test
    void calculate_computesForwardPeForFy1AndFy2() {
        List<EarningsEstimateDto> estimates = List.of(
                createEstimate("annual", LocalDate.of(2025, 12, 31), 5.0, 4.5, 5.5),
                createEstimate("annual", LocalDate.of(2026, 12, 31), 6.25, 5.8, 7.0)
        );

        ValuationScenarioDto result = calculator.calculate(100.0f, estimates, LocalDate.of(2025, 1, 1), null);

        assertThat(result.forwardPeFy1()).isEqualTo(20.0f); // 100 / 5.0
        assertThat(result.forwardPeFy2()).isEqualTo(16.0f); // 100 / 6.25
    }

    @Test
    void calculate_detectsEpsReconciliationDivergenceWhenQuarterlySumDiffers() {
        List<EarningsEstimateDto> estimates = List.of(
                createEstimate("annual", LocalDate.of(2025, 12, 31), 10.0, 9.0, 11.0),
                createEstimate("quarter", LocalDate.of(2025, 3, 31), 2.0, 1.8, 2.2),
                createEstimate("quarter", LocalDate.of(2025, 6, 30), 2.0, 1.8, 2.2),
                createEstimate("quarter", LocalDate.of(2025, 9, 30), 2.0, 1.8, 2.2),
                createEstimate("quarter", LocalDate.of(2025, 12, 31), 2.0, 1.8, 2.2)
        ); // sum = 8.0 vs annual = 10.0 -> diff is 20% (> 8%)

        ValuationScenarioDto result = calculator.calculate(100.0f, estimates, LocalDate.of(2025, 1, 1), null);

        assertThat(result.flagEpsReconciliationDivergence()).isTrue();
        assertThat(result.epsReconciliationNote()).contains("deviates by 20.0%");
    }

    @Test
    void calculate_noEpsReconciliationDivergenceWhenSumMatches() {
        List<EarningsEstimateDto> estimates = List.of(
                createEstimate("annual", LocalDate.of(2025, 12, 31), 8.0, 7.5, 8.5),
                createEstimate("quarter", LocalDate.of(2025, 3, 31), 2.0, 1.8, 2.2),
                createEstimate("quarter", LocalDate.of(2025, 6, 30), 2.0, 1.8, 2.2),
                createEstimate("quarter", LocalDate.of(2025, 9, 30), 2.0, 1.8, 2.2),
                createEstimate("quarter", LocalDate.of(2025, 12, 31), 2.0, 1.8, 2.2)
        ); // sum = 8.0 vs annual = 8.0 -> diff = 0%

        ValuationScenarioDto result = calculator.calculate(100.0f, estimates, LocalDate.of(2025, 1, 1), null);

        assertThat(result.flagEpsReconciliationDivergence()).isFalse();
        assertThat(result.epsReconciliationNote()).isNull();
    }

    @Test
    void calculate_buildsSensitivityMatrixWithCorrectMoves() {
        List<EarningsEstimateDto> estimates = List.of(
                createEstimate("annual", LocalDate.of(2025, 12, 31), 10.0, 8.0, 12.0)
        );
        List<Float> multiples = List.of(10.0f, 20.0f);

        ValuationScenarioDto result = calculator.calculate(100.0f, estimates, LocalDate.of(2025, 1, 1), null, multiples);

        assertThat(result.sensitivityMatrix()).isNotNull();
        List<SensitivityCell> cells = result.sensitivityMatrix().cells();
        assertThat(cells).hasSize(6);

        // Multiple 10x, AVG EPS 10.0 -> implied = 100.0, pctMove = 0.0%
        SensitivityCell cell10Avg = cells.stream()
                .filter(c -> c.multiple().equals(10.0f) && "AVG".equals(c.epsScenario()))
                .findFirst().orElseThrow();
        assertThat(cell10Avg.impliedPrice()).isEqualTo(100.0f);
        assertThat(cell10Avg.pctMoveVsCurrent()).isEqualTo(0.0f);

        // Multiple 20x, AVG EPS 10.0 -> implied = 200.0, pctMove = +100.0%
        SensitivityCell cell20Avg = cells.stream()
                .filter(c -> c.multiple().equals(20.0f) && "AVG".equals(c.epsScenario()))
                .findFirst().orElseThrow();
        assertThat(cell20Avg.impliedPrice()).isEqualTo(200.0f);
        assertThat(cell20Avg.pctMoveVsCurrent()).isEqualTo(100.0f);
    }

    @Test
    void calculate_flagsReverseDcfPeDivergence() {
        List<EarningsEstimateDto> estimates = List.of(
                createEstimate("annual", LocalDate.of(2025, 12, 31), 10.0, 8.0, 12.0)
        );
        // Price = 100, EPS = 10 -> forward P/E = 10 (< 25)
        // Reverse DCF has high implied FCF growth (> 25%)
        ReverseDcfDto dcf = new ReverseDcfDto(
                100.0, 10.0, 0.1, 0.1, 10, 0.03, 30.0, 12.0, "5Y", false, "AGGRESSIVE growth required", null
        );

        ValuationScenarioDto result = calculator.calculate(100.0f, estimates, LocalDate.of(2025, 1, 1), dcf);

        assertThat(result.flagReverseDcfPeDivergence()).isTrue();
    }

    @Test
    void calculate_buildsSensitivityMatrix_fy2Percentages_withExactValues() {
        List<EarningsEstimateDto> estimates = List.of(
                createEstimate("annual", LocalDate.of(2025, 12, 31), 10.0, 9.0, 11.0),
                createEstimate("annual", LocalDate.of(2026, 12, 31), 15.91, 12.85, 18.75)
        );
        List<Float> multiples = List.of(12.0f, 16.0f, 20.0f);

        ValuationScenarioDto result = calculator.calculate(212.21f, estimates, LocalDate.of(2025, 1, 1), null, multiples);

        assertThat(result.sensitivityMatrix()).isNotNull();
        List<SensitivityCell> cells = result.sensitivityMatrix().cells();

        // 12x multiple with LOW EPS (12.85) at 212.21: implied ~154.20 -> -27%
        SensitivityCell cell12Low = cells.stream()
                .filter(c -> c.multiple().equals(12.0f) && "LOW".equals(c.epsScenario()))
                .findFirst().orElseThrow();
        assertThat(cell12Low.pctMoveVsCurrent()).isCloseTo(-27.3f, within(0.5f));
        assertThat(Math.round(cell12Low.pctMoveVsCurrent())).isEqualTo(-27);

        // 20x multiple with HIGH EPS (18.75) at 212.21: implied 375.00 -> +77%
        SensitivityCell cell20High = cells.stream()
                .filter(c -> c.multiple().equals(20.0f) && "HIGH".equals(c.epsScenario()))
                .findFirst().orElseThrow();
        assertThat(cell20High.pctMoveVsCurrent()).isCloseTo(76.7f, within(0.5f));
        assertThat(Math.round(cell20High.pctMoveVsCurrent())).isEqualTo(77);
    }

    private EarningsEstimateDto createEstimate(String horizon, LocalDate date, double avg, double low, double high) {
        return new EarningsEstimateDto(
                date,
                horizon,
                BigDecimal.valueOf(avg),
                BigDecimal.valueOf(high),
                BigDecimal.valueOf(low),
                null, null, null, null, null, null, null, null, null,
                null, null, null, null
        );
    }
}
