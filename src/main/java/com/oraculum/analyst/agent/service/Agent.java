package com.oraculum.analyst.agent.service;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.api.domain.AgentType;

public interface Agent<T> {
    AgentType getName();

    AgentOutput<T> run(AgentContext ctx);

    default String appendCriticFeedbackIfPresent(String prompt, AgentContext ctx) {
        String criticFeedback = ctx.state().getCriticFeedbackFor(getName());
        if (criticFeedback != null) {
            return prompt + "\n\n### CRITIC FEEDBACK (Correction Required)\n"
                    + criticFeedback
                    + "\nPlease correct the identified issue in your revised analysis.";
        }
        return prompt;
    }
}
