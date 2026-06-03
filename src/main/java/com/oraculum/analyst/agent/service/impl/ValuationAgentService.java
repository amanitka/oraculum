package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.ValuationAgentOutput;
import com.oraculum.analyst.agent.service.AgentService;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.AgentType;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ValuationAgentService implements AgentService<ValuationAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public AgentType getName() {
        return AgentType.VALUATION;
    }

    @Override
    public Class<ValuationAgentOutput> getOutputModel() {
        return ValuationAgentOutput.class;
    }

    @Override
    public AgentOutput<ValuationAgentOutput> run(AgentContext ctx) {
        CompanyFactSheetData factSheet = ctx.factSheetData();

        Map<String, Object> promptData = Map.of("derived_metrics",
                factSheet.getDerivedMetrics(ctx.statementVariant()),
                "share_price_signals",
                factSheet.getSharePriceSignals());

        String promptDataJson;
        try {
            promptDataJson = objectMapper.writeValueAsString(promptData);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }

        String prompt = promptRegistry.getPrompt(PromptType.VALUATION).replace("{{ fact_sheet_json }}", promptDataJson);

        String userPrompt = String.format(
                "Analyze the valuation for %s as of %s based on the provided financial fact sheet.",
                ctx.ticker(),
                ctx.requestDate());

        String fullPrompt = prompt + "\n" + userPrompt;

        LlmResponse<ValuationAgentOutput> response = llmRouterApi.executeCall(LlmTierType.MINI,
                fullPrompt,
                ValuationAgentOutput.class);

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}