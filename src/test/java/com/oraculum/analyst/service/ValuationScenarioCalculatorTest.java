package com.oraculum.analyst.service;

import com.oraculum.analyst.dto.ValuationScenarioDto;
import com.oraculum.analyst.dto.ValuationScenarioDto.ImpliedEpsEntry;
import com.oraculum.analyst.dto.ValuationScenarioDto.MatrixSummary;
import com.oraculum.analyst.dto.ValuationScenarioDto.ReverseDcfSummary;
import com.oraculum.analyst.dto.ValuationScenarioDto.SensitivityCell;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.company.api.dto.HistoricalValuationSummaryDto;
import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.company.api.dto.ReverseDcfMatrixCellDto;
import com.oraculum.company.api.dto.ReverseDcfSensitivityMatrixDto;
import com.oraculum.company.api.dto.ValuationZone;
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
    void calculate_detectsEpsReconciliationDivergenceWhenQuarterlySumDiffersMaterially() {
        List<EarningsEstimateDto> estimates = List.of(
                createEstimate("annual", LocalDate.of(2025, 12, 31), 10.0, 9.0, 11.0),
                createEstimate("quarter", LocalDate.of(2025, 3, 31), 2.0, 1.8, 2.2),
                createEstimate("quarter", LocalDate.of(2025, 6, 30), 2.0, 1.8, 2.2),
                createEstimate("quarter", LocalDate.of(2025, 9, 30), 2.0, 1.8, 2.2),
                createEstimate("quarter", LocalDate.of(2025, 12, 31), 2.0, 1.8, 2.2)
        ); // sum = 8.0 vs annual = 10.0 -> diff is 20% (> 15%)

        ValuationScenarioDto result = calculator.calculate(100.0f, estimates, LocalDate.of(2025, 1, 1), null);

        assertThat(result.flagEpsReconciliationDivergence()).isTrue();
        assertThat(result.epsReconciliationStatus()).isEqualTo("MATERIAL_DIVERGENCE");
        assertThat(result.epsReconciliationNote()).contains("deviates by 20.0%");
    }

    @Test
    void calculate_combinesActualQuartersWithRemainingEstimatesForMidYearReconciliation() {
        // NVDA scenario: analysisDate = Oct 28, 2026; FY ends Jan 31, 2027 (annual consensus = 9.31)
        // Q1 (Apr 2026 = 1.59) and Q2 (Jul 2026 = 1.65) are reported actuals; Q3 (Oct 2026 = 2.47) and Q4 (Jan 2027 = 2.75) are future estimates
        // Combined 4Q = 1.59 + 1.65 + 2.47 + 2.75 = 8.46 vs 9.31 (9.1% GAAP vs non-GAAP difference -> not a material error!)
        List<EarningsEstimateDto> estimates = List.of(
                createEstimate("fiscal year", LocalDate.of(2027, 1, 31), 9.31, 8.80, 9.90),
                createEstimate("fiscal quarter", LocalDate.of(2026, 10, 31), 2.47, 2.35, 2.60),
                createEstimate("fiscal quarter", LocalDate.of(2027, 1, 31), 2.75, 2.60, 2.95)
        );
        List<CompanyFinancialRatiosDto> quarterlyRatios = List.of(
                createQuarterlyRatio(LocalDate.of(2026, 7, 31), 1.65f),
                createQuarterlyRatio(LocalDate.of(2026, 4, 30), 1.59f)
        );

        ValuationScenarioDto result = calculator.calculate(
                210.96f, estimates, LocalDate.of(2026, 10, 28), null, quarterlyRatios, List.of()
        );

        assertThat(result.flagEpsReconciliationDivergence()).isFalse();
        assertThat(result.epsReconciliationStatus()).isEqualTo("GAAP_NON_GAAP_BASIS_DIFF");
        assertThat(result.epsReconciliationNote())
                .contains("2Q GAAP actuals")
                .contains("2Q consensus estimates")
                .contains("forward P/E is reliable");
    }

    @Test
    void calculate_incompleteQuartersWithoutActuals_doesNotRaiseFalseDivergenceFlag() {
        // Only 2 future quarterly estimates in current fiscal year and no actuals provided
        List<EarningsEstimateDto> estimates = List.of(
                createEstimate("fiscal year", LocalDate.of(2027, 1, 31), 9.31, 8.80, 9.90),
                createEstimate("fiscal quarter", LocalDate.of(2026, 10, 31), 2.47, 2.35, 2.60),
                createEstimate("fiscal quarter", LocalDate.of(2027, 1, 31), 2.75, 2.60, 2.95)
        );

        ValuationScenarioDto result = calculator.calculate(210.96f, estimates, LocalDate.of(2026, 10, 28), null);

        assertThat(result.flagEpsReconciliationDivergence()).isFalse();
        assertThat(result.epsReconciliationStatus()).isEqualTo("INCOMPLETE_QUARTERS");
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
        assertThat(result.epsReconciliationStatus()).isEqualTo("ALIGNED");
        assertThat(result.epsReconciliationNote()).isNull();
    }

    @Test
    void calculate_buildsSensitivityMatrixWithImpliedEpsAndMatrixSummary() {
        List<EarningsEstimateDto> estimates = List.of(
                createEstimate("annual", LocalDate.of(2025, 12, 31), 10.0, 8.0, 12.0)
        );
        List<Float> multiples = List.of(10.0f, 20.0f);

        ValuationScenarioDto result = calculator.calculate(100.0f, estimates, LocalDate.of(2025, 1, 1), null, multiples);

        assertThat(result.sensitivityMatrix()).isNotNull();
        List<SensitivityCell> cells = result.sensitivityMatrix().cells();
        assertThat(cells).hasSize(6);

        List<ImpliedEpsEntry> impliedEps = result.sensitivityMatrix().impliedEpsByMultiple();
        assertThat(impliedEps).hasSize(2);
        // At 20x multiple and price 100, required EPS = 5.0 vs consensus 10.0 (-50%)
        ImpliedEpsEntry entry20x = impliedEps.stream()
                .filter(e -> e.multiple().equals(20.0f))
                .findFirst().orElseThrow();
        assertThat(entry20x.requiredEpsForCurrentPrice()).isEqualTo(5.0f);
        assertThat(entry20x.consensusVsRequiredPct()).isEqualTo(100.0f);

        MatrixSummary summary = result.sensitivityMatrix().matrixSummary();
        assertThat(summary).isNotNull();
        assertThat(summary.breakEvenPeOnAvgEps()).isEqualTo(10.0f);
        assertThat(summary.minPctMove()).isEqualTo(-20.0f); // 10x * 8.0 = 80 vs 100
        assertThat(summary.maxPctMove()).isEqualTo(140.0f); // 20x * 12.0 = 240 vs 100
    }

    @Test
    void calculate_anchorsMultiplesFromHistoricalValuationData() {
        List<EarningsEstimateDto> estimates = List.of(
                createEstimate("annual", LocalDate.of(2025, 12, 31), 10.0, 8.0, 12.0),
                createEstimate("annual", LocalDate.of(2026, 12, 31), 15.0, 13.0, 17.0)
        );
        List<HistoricalValuationSummaryDto> hist = List.of(
                new HistoricalValuationSummaryDto("P/E", 29.2f, 45.0f, 49.8f, 5, 26.8f, 110.0f)
        );

        ValuationScenarioDto result = calculator.calculate(
                210.0f, estimates, LocalDate.of(2025, 1, 1), null, List.of(), hist
        );

        assertThat(result.sensitivityMatrix().referenceMultiples())
                .containsExactly(20.1f, 25.0f, 29.2f);
        assertThat(result.sensitivityMatrix().multipleAnchors()).hasSize(3);
    }

    @Test
    void calculate_flagsReverseDcfPeDivergenceFromMatrixBaseCaseZone() {
        List<EarningsEstimateDto> estimates = List.of(
                createEstimate("annual", LocalDate.of(2025, 12, 31), 9.31, 8.8, 9.9)
        );
        // Forward P/E = 210.96 / 9.31 = 22.66 (< 25)
        // Base case cell is AGGRESSIVE with 16.68% implied FCF CAGR
        ReverseDcfSensitivityMatrixDto dcfMatrix = new ReverseDcfSensitivityMatrixDto(
                List.of(10.0), List.of(3.0),
                List.of(
                        new ReverseDcfMatrixCellDto(10.0, 3.0, 16.68, ValuationZone.AGGRESSIVE, true),
                        new ReverseDcfMatrixCellDto(11.0, 3.0, 20.71, ValuationZone.SPECULATIVE, false)
                )
        );
        ReverseDcfDto dcf = new ReverseDcfDto(
                5.1e12, 9.6e10, 0.018, 0.1, 10, 0.03, 16.68, 83.1, "5Y", false,
                "Market implies 16.68% annual FCF growth", dcfMatrix
        );

        ValuationScenarioDto result = calculator.calculate(210.96f, estimates, LocalDate.of(2025, 1, 1), dcf);

        assertThat(result.flagReverseDcfPeDivergence()).isTrue();
        assertThat(result.valuationMethodTensionNote())
                .contains("AGGRESSIVE")
                .contains("16.7%");
        ReverseDcfSummary dcfSummary = result.reverseDcfSummary();
        assertThat(dcfSummary).isNotNull();
        assertThat(dcfSummary.baseCaseValuationZone()).isEqualTo("AGGRESSIVE");
        assertThat(dcfSummary.aggressiveOrSpeculativeCellsPct()).isEqualTo(100);
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
