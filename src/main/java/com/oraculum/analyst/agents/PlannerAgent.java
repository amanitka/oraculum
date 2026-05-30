package com.oraculum.analyst.agents;

import com.oraculum.analyst.agents.base.Agent;
import com.oraculum.analyst.agents.base.AgentOutput;
import com.oraculum.analyst.agents.context.AgentContext;
import com.oraculum.analyst.agents.models.PlannerPlan;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.analyst.domain.StatementTemplate;
import com.oraculum.company.api.dto.TickerDto;
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
        StatementTemplate resolvedTemplate = ctx.getTools().resolveTemplate(ctx.getTicker());
        String sharePriceSignals = ctx.getTools().getSharePriceSignals(ctx.getTicker(), ctx.getMarket(), ctx.getAsOf());

        String prompt = systemPrompt.replace("{{ market_signals_json }}", sharePriceSignals);

        String userMessage = String.format("""
                        Ticker: %s
                        Profile: %s
                        Resolved Template must be: %s
                        Please generate the plan \
                        using default variants (annual for fundamentals/cash_flow, ttm for valuation, \
                        quarterly for risk), and set an analysis focus based on the market signals.""",
                ctx.getTicker(),
                profile,
                resolvedTemplate);

        LlmResponse<PlannerPlan> response = ctx.getLlm()
                .executeCall(LlmTierType.STANDARD, prompt + "\n" + userMessage, PlannerPlan.class);

        return new AgentOutput<>(response.result(), response.getTotalTokens());
    }
}
