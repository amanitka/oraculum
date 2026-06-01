package com.oraculum.analyst.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FundamentalsOutput {
    @JsonProperty("income_statement_analysis")
    private AnalysisSection incomeStatementAnalysis;
    @JsonProperty("balance_sheet_analysis")
    private AnalysisSection balanceSheetAnalysis;
    @JsonProperty("financial_ratios_analysis")
    private AnalysisSection financialRatiosAnalysis;
}