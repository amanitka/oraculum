package com.oraculum.analyst.agents;

import com.oraculum.analyst.agents.base.Agent;
import com.oraculum.analyst.agents.base.AgentOutput;
import com.oraculum.analyst.agents.context.AgentContext;
import com.oraculum.analyst.agents.models.PlannerPlan;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.IncomeStatementTemplate;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.company.api.dto.TickerDto;
import com.oraculum.llm.api.dto.LlmTierType;
import org.springframework.stereotype.Component;

@Component
public class PlannerAgent implements Agent<PlannerPlan> {
    private final String systemPrompt;

    public PlannerAgent(PromptRegistry registry) {
        this.systemPrompt = registry.getPrompt(PromptType.PLANNER);
    }

    @Override
    public String getName() {
        return "Planner";
    }

    @Override
    public Class<PlannerPlan> getOutputModel() {
        return PlannerPlan.class;
    }

    @Override
    public AgentOutput<PlannerPlan> run(AgentContext ctx) {
        TickerDto profile = ctx.getTools().getTickerProfile(ctx.getTicker());
        IncomeStatementTemplate resolvedTemplate = ctx.getTools().resolveTemplate(ctx.getTicker());
        String sharePriceSignals = ctx.getTools().getSharePriceSignals(ctx.getTicker(), ctx.getMarket(), ctx.getAsOf());

        String prompt = systemPrompt.replace("{{ market_signals_json }}", sharePriceSignals);

        String userMessage = String.format(
                "Ticker: %s\nProfile: %s\nResolved Template must be: %s\nPlease generate the plan using default variants (annual for fundamentals/cash_flow, ttm for valuation, quarterly for risk), and set an analysis focus based on the market signals.",
                ctx.getTicker(),
                profile,
                resolvedTemplate
        );

        PlannerPlan result = ctx.getLlm().generate(LlmTierType.STANDARD, prompt + "\n" + userMessage, PlannerPlan.class);

        // Token usage is not available in the current LlmRouterApi, so we pass 0.
        return new AgentOutput<>(result, 0);
    }
}
