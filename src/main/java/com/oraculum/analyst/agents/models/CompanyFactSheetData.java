package com.oraculum.analyst.agents.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record CompanyFactSheetData(@JsonProperty("company_profile") Map<String, String> companyProfile,
                                   @JsonProperty("income_statement_history") String incomeStatementHistory,
                                   @JsonProperty("balance_sheet_history") String balanceSheetHistory,
                                   @JsonProperty("cash_flow_history") String cashFlowHistory,
                                   @JsonProperty("derived_metrics") String derivedMetrics,
                                   @JsonProperty("share_price_signals") String sharePriceSignals,
                                   @JsonProperty("recent_news") String recentNews) {
}