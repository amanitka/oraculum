package com.oraculum.analyst.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

public record ValuationScenarioDto(
        @JsonProperty("current_share_price") Float currentSharePrice,
        @JsonProperty("forward_pe_fy1") Float forwardPeFy1,
        @JsonProperty("forward_pe_fy2") Float forwardPeFy2,
        @JsonProperty("next_quarter_end_date") LocalDate nextQuarterEndDate,
        @JsonProperty("next_quarter_eps_consensus") Float nextQuarterEpsConsensus,
        @JsonProperty("next_quarter_revenue_consensus") Float nextQuarterRevenueConsensus,
        @JsonProperty("flag_eps_reconciliation_divergence") Boolean flagEpsReconciliationDivergence,
        @JsonProperty("eps_reconciliation_status") String epsReconciliationStatus,
        @JsonProperty("eps_reconciliation_note") String epsReconciliationNote,
        @JsonProperty("sensitivity_matrix") SensitivityMatrix sensitivityMatrix,
        @JsonProperty("reverse_dcf_summary") ReverseDcfSummary reverseDcfSummary,
        @JsonProperty("flag_reverse_dcf_pe_divergence") Boolean flagReverseDcfPeDivergence,
        @JsonProperty("valuation_method_tension_note") String valuationMethodTensionNote
) {
    public ValuationScenarioDto(
            Float currentSharePrice,
            Float forwardPeFy1,
            Float forwardPeFy2,
            LocalDate nextQuarterEndDate,
            Float nextQuarterEpsConsensus,
            Float nextQuarterRevenueConsensus,
            Boolean flagEpsReconciliationDivergence,
            String epsReconciliationNote,
            SensitivityMatrix sensitivityMatrix,
            Boolean flagReverseDcfPeDivergence
    ) {
        this(
                currentSharePrice,
                forwardPeFy1,
                forwardPeFy2,
                nextQuarterEndDate,
                nextQuarterEpsConsensus,
                nextQuarterRevenueConsensus,
                flagEpsReconciliationDivergence,
                null,
                epsReconciliationNote,
                sensitivityMatrix,
                null,
                flagReverseDcfPeDivergence,
                null
        );
    }

    public static ValuationScenarioDto empty() {
        return new ValuationScenarioDto(
                null, null, null, null, null, null, false, null, null, null, null, false, null
        );
    }

    public record SensitivityMatrix(
            @JsonProperty("target_fiscal_year") String targetFiscalYear,
            @JsonProperty("eps_low") Float epsLow,
            @JsonProperty("eps_avg") Float epsAvg,
            @JsonProperty("eps_high") Float epsHigh,
            @JsonProperty("reference_multiples") List<Float> referenceMultiples,
            @JsonProperty("multiple_anchors") List<MultipleAnchor> multipleAnchors,
            @JsonProperty("implied_eps_by_multiple") List<ImpliedEpsEntry> impliedEpsByMultiple,
            @JsonProperty("matrix_summary") MatrixSummary matrixSummary,
            @JsonProperty("cells") List<SensitivityCell> cells,
            @JsonProperty("disclaimer") String disclaimer
    ) {
        public SensitivityMatrix(
                String targetFiscalYear,
                Float epsLow,
                Float epsAvg,
                Float epsHigh,
                List<Float> referenceMultiples,
                List<SensitivityCell> cells,
                String disclaimer
        ) {
            this(
                    targetFiscalYear,
                    epsLow,
                    epsAvg,
                    epsHigh,
                    referenceMultiples,
                    List.of(),
                    List.of(),
                    null,
                    cells,
                    disclaimer
            );
        }
    }

    public record MultipleAnchor(
            @JsonProperty("multiple") Float multiple,
            @JsonProperty("label") String label
    ) {}

    public record ImpliedEpsEntry(
            @JsonProperty("multiple") Float multiple,
            @JsonProperty("anchor_label") String anchorLabel,
            @JsonProperty("required_eps_for_current_price") Float requiredEpsForCurrentPrice,
            @JsonProperty("consensus_eps_avg") Float consensusEpsAvg,
            @JsonProperty("consensus_vs_required_pct") Float consensusVsRequiredPct
    ) {}

    public record MatrixSummary(
            @JsonProperty("min_pct_move") Float minPctMove,
            @JsonProperty("max_pct_move") Float maxPctMove,
            @JsonProperty("median_pct_move") Float medianPctMove,
            @JsonProperty("base_case_multiple") Float baseCaseMultiple,
            @JsonProperty("base_case_implied_price") Float baseCaseImpliedPrice,
            @JsonProperty("base_case_pct_move") Float baseCasePctMove,
            @JsonProperty("break_even_pe_on_avg_eps") Float breakEvenPeOnAvgEps
    ) {}

    public record ReverseDcfSummary(
            @JsonProperty("base_case_implied_growth_pct") Float baseCaseImpliedGrowthPct,
            @JsonProperty("base_case_valuation_zone") String baseCaseValuationZone,
            @JsonProperty("min_implied_growth_pct") Float minImpliedGrowthPct,
            @JsonProperty("max_implied_growth_pct") Float maxImpliedGrowthPct,
            @JsonProperty("median_implied_growth_pct") Float medianImpliedGrowthPct,
            @JsonProperty("aggressive_or_speculative_cells_pct") Integer aggressiveOrSpeculativeCellsPct,
            @JsonProperty("historical_fcf_cagr_pct") Float historicalFcfCagrPct
    ) {}

    public record SensitivityCell(
            @JsonProperty("multiple") Float multiple,
            @JsonProperty("eps_scenario") String epsScenario,
            @JsonProperty("eps_value") Float epsValue,
            @JsonProperty("implied_price") Float impliedPrice,
            @JsonProperty("pct_move_vs_current") Float pctMoveVsCurrent
    ) {}
}
