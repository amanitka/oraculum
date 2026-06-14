package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.ValuationAgentOutput;
import com.oraculum.analyst.agent.service.Agent;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValuationAgent implements Agent<ValuationAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;

    @Override
    public AgentType getName() {
        return AgentType.VALUATION;
    }



    @Override
    public AgentOutput<ValuationAgentOutput> run(AgentContext ctx) {
        CompanyFactSheetData factSheet = ctx.factSheetData();
        StatementVariant variant = ctx.getVariantFor(getName());

        String recentDailyJson = ctx.factSheetData().getDailySharePriceSignalsList() == null ? "[]" :
                com.oraculum.analyst.util.JsonUtils.toJson(new tools.jackson.databind.ObjectMapper(),
                        ctx.factSheetData().getDailySharePriceSignalsList().stream().limit(5).collect(java.util.stream.Collectors.toList()), "[]");

        String prompt = promptRegistry.getPrompt(PromptType.VALUATION)
                .replace("{{ analysis_focus }}", ctx.analysisFocus() != null ? ctx.analysisFocus() : "Standard comprehensive analysis.")
                .replace("{{ company_financial_ratios }}", factSheet.getCompanyFinancialRatios(variant))
                .replace("{{ daily_share_price_signals }}", recentDailyJson);

        String userPrompt = String.format(
                "Analyze the valuation for %s as of %s based on the provided financial fact sheet.",
                ctx.ticker(),
                ctx.analysisDate());

        String fullPrompt = appendCriticFeedbackIfPresent(prompt + "\n" + userPrompt, ctx);

        LlmResponse<ValuationAgentOutput> response = llmRouterApi.executeCall(LlmTierType.STANDARD,
                fullPrompt,
                ValuationAgentOutput.class);

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}