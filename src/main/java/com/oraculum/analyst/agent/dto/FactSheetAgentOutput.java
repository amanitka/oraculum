package com.oraculum.analyst.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FactSheetAgentOutput(@JsonProperty("fact_sheet") CompanyFactSheetData factSheet) {
}