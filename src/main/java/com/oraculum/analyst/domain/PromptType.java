package com.oraculum.analyst.domain;

import lombok.Getter;

@Getter
public enum PromptType {
    PLANNER("/prompt/planner.md"),
    NEWS("/prompt/news.md"),
    RISK("/prompt/risk.md"),
    CRITIC("/prompt/critic.md"),
    CASH_FLOW("/prompt/cash_flow.md"),
    VALUATION("/prompt/valuation.md"),
    SHARE_PRICE("/prompt/share_price.md"),
    SYNTHESIZER("/prompt/synthesizer.md"),
    FUNDAMENTALS("/prompt/fundamentals.md");

    private final String path;

    PromptType(String path) {
        this.path = path;
    }
}
