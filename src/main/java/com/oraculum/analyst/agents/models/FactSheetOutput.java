package com.oraculum.analyst.agents.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FactSheetOutput {
    @JsonProperty("fact_sheet")
    FinancialFactSheet factSheet;
}
