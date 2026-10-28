package com.oraculum.analyst.service.valuation;

import com.oraculum.analyst.dto.ValuationScenarioDto.ImpliedEpsEntry;
import com.oraculum.analyst.dto.ValuationScenarioDto.MatrixSummary;
import com.oraculum.analyst.dto.ValuationScenarioDto.MultipleAnchor;
import com.oraculum.analyst.dto.ValuationScenarioDto.SensitivityMatrix;
import com.oraculum.company.api.dto.HistoricalValuationSummaryDto;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SensitivityMatrixCalculatorTest {

    private SensitivityMatrixCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new SensitivityMatrixCalculator();
    }

    @Test
    void calculate_nullTarget_returnsNull() {
        SensitivityMatrix matrix = calculator.calculate(100.0f, null, List.of(), List.of());
        assertThat(matrix).isNull();
    }

    @Test
    void calculate_withExplicitMultiples_buildsCellsAndImpliedEps() {
        EarningsEstimateDto target = createEstimate(LocalDate.of(2027, 12, 31), 10.0, 8.0, 12.0);
        List<Float> explicit = List.of(15.0f, 25.0f);

        SensitivityMatrix matrix = calculator.calculate(150.0f, target, explicit, List.of());

        assertThat(matrix).isNotNull();
        assertThat(matrix.referenceMultiples()).containsExactly(15.0f, 25.0f);
        assertThat(matrix.cells()).hasSize(6);

        List<ImpliedEpsEntry> impliedEps = matrix.impliedEpsByMultiple();
        assertThat(impliedEps).hasSize(2);

        // At 15x multiple and price 150 -> required EPS = 10.0, consensus = 10.0 -> diff = 0%
        ImpliedEpsEntry entry15 = impliedEps.getFirst();
        assertThat(entry15.requiredEpsForCurrentPrice()).isEqualTo(10.0f);
        assertThat(entry15.consensusVsRequiredPct()).isEqualTo(0.0f);

        MatrixSummary summary = matrix.matrixSummary();
        assertThat(summary).isNotNull();
        assertThat(summary.breakEvenPeOnAvgEps()).isEqualTo(15.0f);
    }

    @Test
    void resolveMultipleAnchors_withElevatedHistoricalMin_appliesCyclicalCompression() {
        // NVDA-like situation: 10Y min is 26.8x (> 15x) -> low anchor should be 26.8 * 0.75 = 20.1x
        HistoricalValuationSummaryDto pe = new HistoricalValuationSummaryDto(
                "P/E", 29.2f, 45.0f, 49.8f, 5, 26.8f, 110.0f
        );

        List<MultipleAnchor> anchors = calculator.resolveMultipleAnchors(List.of(), List.of(pe));

        assertThat(anchors).hasSize(3);
        assertThat(anchors.get(0).multiple()).isEqualTo(20.1f);
        assertThat(anchors.get(0).label()).contains("Cyclical Compression");
        assertThat(anchors.get(1).multiple()).isEqualTo(25.0f);
        assertThat(anchors.get(2).multiple()).isEqualTo(29.2f);
    }

    @Test
    void resolveMultipleAnchors_withoutHistorical_usesDefaults() {
        List<MultipleAnchor> anchors = calculator.resolveMultipleAnchors(List.of(), List.of());

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
