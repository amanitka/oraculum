package com.oraculum.analyst.api.domain;

import com.oraculum.company.api.domain.StatementVariant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum AgentType {
    FUNDAMENTALS("Fundamentals", true, 1, true, Set.of(StatementVariant.QUARTERLY)),
    NEWS("News", true, 5, false, Collections.emptySet()),
    RISK("Risk", true, 6, true, Set.of(StatementVariant.ANNUAL, StatementVariant.TTM)),
    CRITIC("Critic", false, -1, false, Collections.emptySet()),
    CASH_FLOW("CashFlow", true, 2, true, Set.of(StatementVariant.TTM)),
    VALUATION("Valuation", true, 3, true, Set.of(StatementVariant.TTM, StatementVariant.ANNUAL)),
    SHARE_PRICE("SharePrice", true, 4, false, Collections.emptySet()),
    EARNINGS_ESTIMATES("EarningsEstimates", true, 0, false, Collections.emptySet()),
    SYNTHESIZER("Synthesizer", false, -1, false, Collections.emptySet());

    private final String agentName;
    private final boolean specialist;
    private final int executionOrder;
    private final boolean supportsRerun;
    private final Set<StatementVariant> requiredVariants;
}