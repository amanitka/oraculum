package com.oraculum.analyst.api.domain;

import com.oraculum.company.api.domain.StatementVariant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum AgentType {
    FUNDAMENTALS("Fundamentals", true, 1, true, FinancialDataProfile.HISTORICAL_OPERATING),
    NEWS("News", true, 5, false, FinancialDataProfile.MARKET_SIGNALS),
    RISK("Risk", true, 6, true, FinancialDataProfile.BALANCE_SHEET_RISK),
    CRITIC("Critic", false, -1, false, FinancialDataProfile.MARKET_SIGNALS),
    CASH_FLOW("CashFlow", true, 2, true, FinancialDataProfile.CASH_GENERATION),
    VALUATION("Valuation", true, 3, true, FinancialDataProfile.CURRENT_VALUATION),
    SHARE_PRICE("SharePrice", true, 4, false, FinancialDataProfile.MARKET_SIGNALS),
    EARNINGS_ESTIMATES("EarningsEstimates", true, 0, false, FinancialDataProfile.MARKET_SIGNALS),
    SYNTHESIZER("Synthesizer", false, -1, false, FinancialDataProfile.MARKET_SIGNALS);

    private final String agentName;
    private final boolean specialist;
    private final int executionOrder;
    private final boolean supportsRerun;
    private final FinancialDataProfile dataProfile;

    public Set<StatementVariant> requiredVariants() {
        return dataProfile.variants();
    }
}