package com.oraculum.analyst.api.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AgentType {
    FUNDAMENTALS("Fundamentals", true, 1, true),
    PLANNER("Planner", false, -1, false),
    NEWS("News", true, 5, false),
    RISK("Risk", true, 6, true),
    CRITIC("Critic", false, -1, false),
    CASH_FLOW("CashFlow", true, 2, true),
    VALUATION("Valuation", true, 3, true),
    SHARE_PRICE("SharePrice", true, 4, false),
    SYNTHESIZER("Synthesizer", false, -1, false);

    private final String agentName;
    private final boolean specialist;
    private final int executionOrder;
    private final boolean supportsRerun;
}