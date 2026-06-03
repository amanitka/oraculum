package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.company.api.domain.StatementTemplate;
import com.oraculum.company.api.domain.StatementVariant;
import lombok.Data;

@Data
public class PlannerPlan {
    private StatementTemplate template;

    @JsonProperty("fundamentals_variant")
    private StatementVariant fundamentalsVariant = StatementVariant.ANNUAL;

    @JsonProperty("cash_flow_variant")
    private StatementVariant cashFlowVariant = StatementVariant.ANNUAL;

    @JsonProperty("valuation_variant")
    private StatementVariant valuationVariant = StatementVariant.TTM;

    @JsonProperty("risk_variant")
    private StatementVariant riskVariant = StatementVariant.QUARTERLY;

    @JsonProperty("analysis_focus")
    private String analysisFocus =
            "Prioritize recent momentum shifts, valuation dislocations, and unusual volume " + "signals.";
}
