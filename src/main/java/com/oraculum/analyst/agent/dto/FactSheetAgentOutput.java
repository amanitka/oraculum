package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.analyst.dto.CompanyFactSheetData;

public record FactSheetAgentOutput(@JsonProperty("fact_sheet") CompanyFactSheetData factSheet) {
}
