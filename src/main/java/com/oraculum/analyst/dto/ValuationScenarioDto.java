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
        @JsonProperty("eps_reconciliation_note") String epsReconciliationNote,
        @JsonProperty("sensitivity_matrix") SensitivityMatrix sensitivityMatrix,
        @JsonProperty("flag_reverse_dcf_pe_divergence") Boolean flagReverseDcfPeDivergence
) {
    public static ValuationScenarioDto empty() {
        return new ValuationScenarioDto(null, null, null, null, null, null, false, null, null, false);
    }

    public record SensitivityMatrix(
            @JsonProperty("target_fiscal_year") String targetFiscalYear,
            @JsonProperty("eps_low") Float epsLow,
            @JsonProperty("eps_avg") Float epsAvg,
            @JsonProperty("eps_high") Float epsHigh,
            @JsonProperty("reference_multiples") List<Float> referenceMultiples,
            @JsonProperty("cells") List<SensitivityCell> cells,
            @JsonProperty("disclaimer") String disclaimer
    ) {}

    public record SensitivityCell(
            @JsonProperty("multiple") Float multiple,
            @JsonProperty("eps_scenario") String epsScenario,
            @JsonProperty("eps_value") Float epsValue,
            @JsonProperty("implied_price") Float impliedPrice,
            @JsonProperty("pct_move_vs_current") Float pctMoveVsCurrent
    ) {}
}
