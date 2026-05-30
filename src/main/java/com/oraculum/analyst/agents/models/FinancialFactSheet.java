package com.oraculum.analyst.agents.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class FinancialFactSheet {
    @JsonProperty("ticker_profile")
    Map<String, String> tickerProfile;

    @JsonProperty("income_statement_history")
    String incomeStatementHistory;

    @JsonProperty("balance_sheet_history")
    String balanceSheetHistory;

    @JsonProperty("cash_flow_history")
    String cashFlowHistory;

    @JsonProperty("derived_metrics")
    String derivedMetrics;

    @JsonProperty("share_price_signals")
    String sharePriceSignals;
}
