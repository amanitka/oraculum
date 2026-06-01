package com.oraculum.analyst.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AgentType {
    FACT_SHEET("FactSheet"),
    FUNDAMENTALS("Fundamentals"),
    PLANNER("Planner"),
    NEWS("News"),
    RISK("Risk"),
    CRITIC("Critic"),
    CASH_FLOW("CashFlow"),
    VALUATION("Valuation"),
    SHARE_PRICE("SharePrice"),
    SYNTHESIZER("Synthesizer"),
    NEWS_SUMMARY("NewsSummary");

    private final String agentName;
}