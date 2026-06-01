package com.oraculum.analyst.agents.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FundamentalsOutput(
        @JsonProperty("income_statement_analysis") AnalysisSection incomeStatementAnalysis,
        @JsonProperty("balance_sheet_analysis") AnalysisSection balanceSheetAnalysis,
        @JsonProperty("financial_ratios_analysis") AnalysisSection financialRatiosAnalysis
) {
}