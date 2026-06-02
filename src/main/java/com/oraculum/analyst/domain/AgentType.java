package com.oraculum.analyst.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AgentType {
    FACT_SHEET("FactSheet", false),
    FUNDAMENTALS("Fundamentals", true),
    PLANNER("Planner", false),
    NEWS("News", true),
    RISK("Risk", true),
    CRITIC("Critic", false),
    CASH_FLOW("CashFlow", true),
    VALUATION("Valuation", true),
    SHARE_PRICE("SharePrice", true),
    SYNTHESIZER("Synthesizer", false),
    NEWS_SUMMARY("NewsSummary", false);

    private final String agentName;
    private final boolean specialist;
}