package com.oraculum.analyst.api.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AgentType {
    FUNDAMENTALS("Fundamentals", true, 1),
    PLANNER("Planner", false, -1),
    NEWS("News", true, 5),
    RISK("Risk", true, 6),
    CRITIC("Critic", false, -1),
    CASH_FLOW("CashFlow", true, 2),
    VALUATION("Valuation", true, 3),
    SHARE_PRICE("SharePrice", true, 4),
    SYNTHESIZER("Synthesizer", false, -1),
    NEWS_SUMMARY("NewsSummary", false, -1);

    private final String agentName;
    private final boolean specialist;
    private final int executionOrder;
}