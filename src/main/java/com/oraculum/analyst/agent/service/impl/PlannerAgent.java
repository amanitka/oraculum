package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.PlannerPlan;
import com.oraculum.analyst.agent.service.Agent;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import org.springframework.stereotype.Component;

@Component
public class PlannerAgent implements Agent<PlannerPlan> {
    private final LlmRouterApi llmRouterApi;
    private final String systemPrompt;

    public PlannerAgent(LlmRouterApi llmRouterApi, PromptRegistry registry) {
        this.llmRouterApi = llmRouterApi;
        this.systemPrompt = registry.getPrompt(PromptType.PLANNER);
    }

    @Override
    public AgentType getName() {
        return AgentType.PLANNER;
    }



    @Override
    public AgentOutput<PlannerPlan> run(AgentContext ctx) {
        String prompt = systemPrompt.replace("{{ daily_share_price_signals }}",
                        ctx.factSheetData().getDailySharePriceSignalsForPlanner())
                .replace("{{ company_profile }}", ctx.factSheetData().getCompanyProfile())
                .replace("{{ ticker }}", ctx.ticker());

        LlmResponse<PlannerPlan> response = llmRouterApi.executeCall(LlmTierType.STANDARD,
                prompt,
                PlannerPlan.class);

        return new AgentOutput<>(response.result(), response.getTotalTokens());
    }
}