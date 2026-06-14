package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.SharePriceAgentOutput;
import com.oraculum.analyst.agent.service.Agent;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SharePriceAgent implements Agent<SharePriceAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;

    @Override
    public AgentType getName() {
        return AgentType.SHARE_PRICE;
    }



    @Override
    public AgentOutput<SharePriceAgentOutput> run(AgentContext ctx) {
        CompanyFactSheetData factSheet = ctx.factSheetData();

        String prompt = promptRegistry.getPrompt(PromptType.SHARE_PRICE)
                .replace("{{ analysis_focus }}", ctx.analysisFocus() != null ? ctx.analysisFocus() : "Standard comprehensive analysis.")
                .replace("{{ daily_share_price_signals }}", factSheet.getDailySharePriceSignals())
                .replace("{{ monthly_share_price_signals }}", factSheet.getMonthlySharePriceSignals())
                .replace("{{ ticker }}", ctx.ticker())
                .replace("{{ analysis_date }}", ctx.analysisDate().toString());

        String fullPrompt = appendCriticFeedbackIfPresent(prompt, ctx);

        LlmResponse<SharePriceAgentOutput> response = llmRouterApi.executeCall(LlmTierType.STANDARD,
                fullPrompt,
                SharePriceAgentOutput.class);

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}