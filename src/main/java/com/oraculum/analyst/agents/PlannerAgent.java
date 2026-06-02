package com.oraculum.analyst.agents;

import com.oraculum.analyst.agents.base.Agent;
import com.oraculum.analyst.agents.base.AgentOutput;
import com.oraculum.analyst.agents.context.AgentContext;
import com.oraculum.analyst.agents.models.PlannerPlan;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.AgentType;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import org.springframework.stereotype.Component;

@Component
public class PlannerAgent implements Agent<PlannerPlan> {
    private final String systemPrompt;

    public PlannerAgent(PromptRegistry registry) {
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
        CompanyDto company = ctx.tools().getCompany(ctx.ticker(), ctx.market());
        String sharePriceSignals = ctx.tools().getSharePriceSignals(ctx.companyId(), ctx.runDateTime());

        String prompt = systemPrompt.replace("{{ market_signals_json }}", sharePriceSignals);

        String userMessage = String.format("""
                        Ticker: %s
                        Profile: %s
                        Please generate the plan \
                        using default variants (annual for fundamentals/cash_flow, ttm for valuation, \
                        quarterly for risk), and set an analysis focus based on the market signals.""",
                ctx.ticker(),
                company);

        LlmResponse<PlannerPlan> response = ctx.llm()
                .executeCall(LlmTierType.STANDARD, prompt + "\n" + userMessage, PlannerPlan.class);

        return new AgentOutput<>(response.result(), response.getTotalTokens());
    }
}