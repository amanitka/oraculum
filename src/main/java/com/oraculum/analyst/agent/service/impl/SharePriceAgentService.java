package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.*;
import com.oraculum.analyst.agent.service.AgentService;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.AgentType;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SharePriceAgentService implements AgentService<SharePriceAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;

    @Override
    public AgentType getName() {
        return AgentType.SHARE_PRICE;
    }

    @Override
    public Class<SharePriceAgentOutput> getOutputModel() {
        return SharePriceAgentOutput.class;
    }

    @Override
    public AgentOutput<SharePriceAgentOutput> run(AgentContext ctx) {
        FactSheetAgentOutput factSheetOutput = (FactSheetAgentOutput) ctx.priorOutputs().get(AgentType.FACT_SHEET);
        CompanyFactSheetData factSheet = factSheetOutput.factSheet();

        String signalsJson = factSheet.sharePriceSignals();

        String prompt = promptRegistry.getPrompt(PromptType.SHARE_PRICE)
                .replace("{{ market_signals_json }}", signalsJson);

        String userPrompt = String.format(
                "Analyze the market signals for %s as of %s based on the provided financial fact sheet.",
                ctx.ticker(),
                ctx.requestDate());

        String fullPrompt = prompt + "\n" + userPrompt;

        LlmResponse<SharePriceAgentOutput> response = llmRouterApi.executeCall(LlmTierType.MINI,
                fullPrompt,
                SharePriceAgentOutput.class);

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}