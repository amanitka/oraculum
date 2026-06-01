package com.oraculum.analyst.agents.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FactSheetOutput(
        @JsonProperty("fact_sheet") FinancialFactSheetData factSheet
) {
}