package com.oraculum.analyst.service.valuation;

import com.oraculum.analyst.dto.valuation.ImpliedEpsEntryDto;
import com.oraculum.analyst.dto.valuation.MatrixSummaryDto;
import com.oraculum.analyst.dto.valuation.MultipleAnchorDto;
import com.oraculum.analyst.dto.valuation.SensitivityMatrixDto;
import com.oraculum.company.api.dto.HistoricalValuationSummaryDto;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SensitivityMatrixCalculatorTest {

    private SensitivityMatrixCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new SensitivityMatrixCalculator();
    }

    @Test
    void calculate_nullTarget_returnsNull() {
        SensitivityMatrixDto matrix = calculator.calculate(100.0f, null, List.of(), List.of());
        assertThat(matrix).isNull();
    }

    @Test
    void calculate_withExplicitMultiplesAndAnalysisDate_populatesRawAndDiscountedMatrixSummary() {
        EarningsEstimateDto target = createEstimate(LocalDate.of(2027, 12, 31), 10.0, 8.0, 12.0);
        List<Float> explicit = List.of(15.0f, 20.0f, 25.0f);

        SensitivityMatrixDto matrix = calculator.calculate(
                150.0f, target, explicit, List.of(), LocalDate.of(2025, 12, 31)
        );

        assertThat(matrix).isNotNull();
        assertThat(matrix.referenceMultiples()).containsExactly(15.0f, 20.0f, 25.0f);
        assertThat(matrix.cells()).hasSize(9);

        List<ImpliedEpsEntryDto> impliedEps = matrix.impliedEpsByMultiple();
        assertThat(impliedEps).hasSize(3);
        ImpliedEpsEntryDto entry15 = impliedEps.getFirst();
        assertThat(entry15.requiredEpsForCurrentPrice()).isEqualTo(10.0f);
        assertThat(entry15.consensusVsRequiredPct()).isEqualTo(0.0f);

        MatrixSummaryDto summary = matrix.matrixSummary();
        assertThat(summary).isNotNull();
        assertThat(summary.breakEvenPeOnAvgEps()).isEqualTo(15.0f);
        assertThat(summary.baseCaseMultiple()).isEqualTo(20.0f);
        assertThat(summary.baseCaseImpliedPrice()).isEqualTo(200.0f);
        // 2 years @ 10% discount factor = 1.21 -> 200 / 1.21 = 165.29 (+10.2% move vs +33.3% undiscounted)
        assertThat(summary.discountedBaseCaseImpliedPrice()).isCloseTo(165.29f, within(0.05f));
        assertThat(summary.discountedBaseCasePctMove()).isCloseTo(10.2f, within(0.2f));
        assertThat(summary.discountedMaxPctMove()).isLessThan(summary.maxPctMove());
    }

    @Test
    void resolveMultipleAnchors_withElevatedHistoricalMin_appliesCyclicalCompression() {
        HistoricalValuationSummaryDto pe = new HistoricalValuationSummaryDto(
                "P/E", 29.2f, 45.0f, 49.8f, 5, 26.8f, 110.0f
        );

        List<MultipleAnchorDto> anchors = calculator.resolveMultipleAnchors(List.of(), List.of(pe));

        assertThat(anchors).hasSize(3);
        assertThat(anchors.get(0).multiple()).isEqualTo(20.1f);
        assertThat(anchors.get(0).label()).contains("Cyclical Compression");
        assertThat(anchors.get(1).multiple()).isEqualTo(25.0f);
        assertThat(anchors.get(2).multiple()).isEqualTo(29.2f);
    }

    @Test
    void resolveMultipleAnchors_withoutHistorical_usesDefaults() {
        List<MultipleAnchorDto> anchors = calculator.resolveMultipleAnchors(List.of(), List.of());

        assertThat(anchors).hasSize(3);
        assertThat(anchors.get(0).multiple()).isEqualTo(12.0f);
        assertThat(anchors.get(1).multiple()).isEqualTo(16.0f);
        assertThat(anchors.get(2).multiple()).isEqualTo(20.0f);
    }

    private EarningsEstimateDto createEstimate(LocalDate date, double avg, double low, double high) {
        return new EarningsEstimateDto(
                date, "annual", BigDecimal.valueOf(avg),
                BigDecimal.valueOf(high), BigDecimal.valueOf(low),
                null, null, null, null, null, null, null, null, null,
                null, null, null, null
        );
    }
}
