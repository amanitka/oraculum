package com.oraculum.analyst.service.valuation;

import com.oraculum.analyst.dto.valuation.ForwardReverseDcfDto;
import com.oraculum.analyst.dto.valuation.MethodDivergenceResult;
import com.oraculum.analyst.dto.valuation.ReverseDcfSummaryDto;
import com.oraculum.company.api.dto.HistoricalValuationSummaryDto;
import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.company.api.dto.ReverseDcfMatrixCellDto;
import com.oraculum.company.api.dto.ReverseDcfSensitivityMatrixDto;
import com.oraculum.company.api.dto.ValuationZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValuationDivergenceEvaluatorTest {

    private ValuationDivergenceEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new ValuationDivergenceEvaluator();
    }

    @Test
    void buildReverseDcfSummary_nullInput_returnsNull() {
        assertThat(evaluator.buildReverseDcfSummary(null)).isNull();
    }

    @Test
    void buildReverseDcfSummary_extractsBaseCaseAndAggressivePercentage() {
        ReverseDcfSensitivityMatrixDto matrix = new ReverseDcfSensitivityMatrixDto(
                List.of(10.0), List.of(3.0),
                List.of(
                        new ReverseDcfMatrixCellDto(10.0, 3.0, 16.68, ValuationZone.AGGRESSIVE, true),
                        new ReverseDcfMatrixCellDto(11.0, 3.0, 20.71, ValuationZone.SPECULATIVE, false)
                )
        );
        ReverseDcfDto dcf = new ReverseDcfDto(
                5.1e12, 9.6e10, 0.018, 0.1, 10, 0.03, 16.68, 83.1, "5Y", false,
                "Aggressive FCF growth implied", matrix
        );

        ReverseDcfSummaryDto summary = evaluator.buildReverseDcfSummary(dcf);

        assertThat(summary).isNotNull();
        assertThat(summary.baseCaseValuationZone()).isEqualTo("AGGRESSIVE");
        assertThat(summary.baseCaseImpliedGrowthPct()).isEqualTo(16.68f);
        assertThat(summary.aggressiveOrSpeculativeCellsPct()).isEqualTo(100);
        assertThat(summary.historicalFcfCagrPct()).isEqualTo(83.1f);
    }

    @Test
    void evaluateMethodDivergence_aggressiveDcfAndLowForwardPe_triggersConflictWhenTwoStageNotFair() {
        ReverseDcfSensitivityMatrixDto matrix = new ReverseDcfSensitivityMatrixDto(
                List.of(10.0), List.of(3.0),
                List.of(new ReverseDcfMatrixCellDto(10.0, 3.0, 16.68, ValuationZone.AGGRESSIVE, true))
        );
        ReverseDcfDto dcf = new ReverseDcfDto(
                5.1e12, 9.6e10, 0.018, 0.1, 10, 0.03, 16.68, 83.1, "5Y", false, "Aggressive", matrix
        );
        List<HistoricalValuationSummaryDto> hist = List.of(
                new HistoricalValuationSummaryDto("P/E", 29.2f, 45.0f, 49.8f, 5, 26.8f, 110.0f)
        );

        MethodDivergenceResult result = evaluator.evaluateMethodDivergence(dcf, 22.7f, hist);

        assertThat(result.divergent()).isTrue();
        assertThat(result.note())
                .contains("METHOD CONFLICT")
                .contains("AGGRESSIVE zone requiring 16.7%")
                .contains("10Y P/E percentile 5%");
    }

    @Test
    void evaluateMethodDivergence_twoStageFairResolvesTrailingAggressiveConflict() {
        ReverseDcfSensitivityMatrixDto matrix = new ReverseDcfSensitivityMatrixDto(
                List.of(10.0), List.of(3.0),
                List.of(new ReverseDcfMatrixCellDto(10.0, 3.0, 16.68, ValuationZone.AGGRESSIVE, true))
        );
        ReverseDcfDto dcf = new ReverseDcfDto(
                5.1e12, 9.6e10, 0.018, 0.1, 10, 0.03, 16.68, 83.1, "5Y", false, "Aggressive", matrix
        );
        ForwardReverseDcfDto twoStage = ForwardReverseDcfDto.builder()
                .impliedGrowthYears3To10Pct(7.2f)
                .relativeValuationZone(ValuationZone.FAIR)
                .build();

        MethodDivergenceResult result = evaluator.evaluateMethodDivergence(dcf, twoStage, 22.7f, List.of());

        assertThat(result.divergent()).isFalse();
        assertThat(result.note())
                .contains("TRAILING VS FORWARD DCF RECONCILED")
                .contains("FAIR zone (7.2% CAGR in Years 3-10)");
    }

    @Test
    void evaluateMethodDivergence_fairDcf_returnsFalse() {
        ReverseDcfSensitivityMatrixDto matrix = new ReverseDcfSensitivityMatrixDto(
                List.of(10.0), List.of(3.0),
                List.of(new ReverseDcfMatrixCellDto(10.0, 3.0, 7.5, ValuationZone.FAIR, true))
        );
        ReverseDcfDto dcf = new ReverseDcfDto(
                1e11, 5e9, 0.05, 0.1, 10, 0.03, 7.5, 10.0, "5Y", false, "Fair", matrix
        );

        MethodDivergenceResult result = evaluator.evaluateMethodDivergence(dcf, 18.0f, List.of());

        assertThat(result.divergent()).isFalse();
        assertThat(result.note()).isNull();
    }
}
