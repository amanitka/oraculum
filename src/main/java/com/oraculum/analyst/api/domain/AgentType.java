package com.oraculum.analyst.api.domain;

import com.oraculum.company.api.domain.StatementVariant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum AgentType {
    MACROECONOMIC("Macroeconomic", true, 0, false, FinancialDataProfile.MARKET_SIGNALS),
    FUNDAMENTALS("Fundamentals", true, 1, true, FinancialDataProfile.HISTORICAL_OPERATING),
    CASH_FLOW("CashFlow", true, 2, true, FinancialDataProfile.CASH_GENERATION),
    VALUATION("Valuation", true, 3, true, FinancialDataProfile.CURRENT_VALUATION),
    SHARE_PRICE("SharePrice", true, 4, false, FinancialDataProfile.MARKET_SIGNALS),
    RISK("Risk", true, 5, true, FinancialDataProfile.BALANCE_SHEET_RISK),
    NEWS("News", true, 6, false, FinancialDataProfile.MARKET_SIGNALS),
    INSIDER_TRANSACTION("InsiderTransaction", true, 7, false, FinancialDataProfile.MARKET_SIGNALS),
    EARNINGS_ESTIMATES("EarningsEstimates", true, 8, false, FinancialDataProfile.MARKET_SIGNALS),
    CRITIC("Critic", false, -1, false, FinancialDataProfile.MARKET_SIGNALS),
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
