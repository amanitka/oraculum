package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.PlannerPlan;
import com.oraculum.analyst.agent.service.AgentService;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.AgentType;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.company.api.dto.CompanyDto;
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
        CompanyDto company = ctx.company();
        String sharePriceSignals = ctx.factSheetData().getSharePriceSignals();
        String companyProfile = ctx.factSheetData().getCompanyProfile();

        String prompt = systemPrompt.replace("{{ market_signals_json }}", sharePriceSignals);

        String userMessage = String.format("""
                        Ticker: %s
                        Profile: %s
                        Please generate the plan \
                        using default variants (annual for fundamentals/cash_flow, ttm for valuation, \
                        quarterly for risk), and set an analysis focus based on the market signals.""",
                ctx.ticker(),
                companyProfile);

        LlmResponse<PlannerPlan> response = llmRouterApi.executeCall(LlmTierType.STANDARD,
                prompt + "\n" + userMessage,
                PlannerPlan.class);

        return new AgentOutput<>(response.result(), response.getTotalTokens());
    }
}