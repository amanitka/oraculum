package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.PlannerPlan;
import com.oraculum.analyst.agent.service.AgentService;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.AgentType;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import org.springframework.stereotype.Component;

@Component
public class PlannerAgentService implements AgentService<PlannerPlan> {
    private final LlmRouterApi llmRouterApi;
    private final String systemPrompt;

    public PlannerAgentService(LlmRouterApi llmRouterApi, PromptRegistry registry) {
        this.llmRouterApi = llmRouterApi;
        this.systemPrompt = registry.getPrompt(PromptType.PLANNER);
    }

    @Override
    public AgentType getName() {
        return AgentType.PLANNER;
    }

    @Override
    public Class<PlannerPlan> getOutputModel() {
        return PlannerPlan.class;
    }

    @Override
    public AgentOutput<PlannerPlan> run(AgentContext ctx) {
        String prompt = systemPrompt.replace("{{ daily_share_price_signals }}",
                        ctx.factSheetData().getDailySharePriceSignals())
                .replace("{{ company_profile }}", ctx.factSheetData().getCompanyProfile());

        String userMessage = String.format("Analyze %s and determine the plan.", ctx.ticker());

        LlmResponse<PlannerPlan> response = llmRouterApi.executeCall(LlmTierType.STANDARD,
                prompt + "\n" + userMessage,
                PlannerPlan.class);

        return new AgentOutput<>(response.result(), response.getTotalTokens());
    }
}