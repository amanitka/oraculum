package com.oraculum.analyst.dto.valuation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDate;

@Builder(toBuilder = true)
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
        @JsonProperty("sensitivity_matrix") SensitivityMatrixDto sensitivityMatrix,
        @JsonProperty("reverse_dcf_summary") ReverseDcfSummaryDto reverseDcfSummary,
        @JsonProperty("two_stage_reverse_dcf") ForwardReverseDcfDto twoStageReverseDcf,
        @JsonProperty("operating_earnings_summary") OperatingEarningsSummaryDto operatingEarningsSummary,
        @JsonProperty("flag_reverse_dcf_pe_divergence") Boolean flagReverseDcfPeDivergence,
        @JsonProperty("valuation_method_tension_note") String valuationMethodTensionNote
) {

    public static ValuationScenarioDto empty() {
        return ValuationScenarioDto.builder()
                .flagEpsReconciliationDivergence(false)
                .flagReverseDcfPeDivergence(false)
                .build();
    }
}
