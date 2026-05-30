package com.oraculum.analyst.agents.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.analyst.domain.IncomeStatementTemplate;
import com.oraculum.analyst.domain.StatementVariant;
import lombok.Data;

@Data
public class PlannerPlan {
    private IncomeStatementTemplate template;

    @JsonProperty("fundamentals_variant")
    private StatementVariant fundamentalsVariant = StatementVariant.ANNUAL;

    @JsonProperty("cash_flow_variant")
    private StatementVariant cashFlowVariant = StatementVariant.ANNUAL;

    @JsonProperty("valuation_variant")
    private StatementVariant valuationVariant = StatementVariant.TTM;

    @JsonProperty("risk_variant")
    private StatementVariant riskVariant = StatementVariant.QUARTERLY;

    @JsonProperty("analysis_focus")
    private String analysisFocus = "Prioritize recent momentum shifts, valuation dislocations, and unusual volume signals.";
}
