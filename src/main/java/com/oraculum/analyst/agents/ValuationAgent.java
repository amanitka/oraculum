package com.oraculum.analyst.agents;

import com.oraculum.analyst.agents.base.Agent;
import com.oraculum.analyst.agents.base.AgentOutput;
import com.oraculum.analyst.agents.context.AgentContext;
import com.oraculum.analyst.agents.models.FactSheetAgentOutput;
import com.oraculum.analyst.agents.models.FinancialFactSheetData;
import com.oraculum.analyst.agents.models.ValuationAgentOutput;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.AgentType;
import com.oraculum.analyst.domain.PromptType;
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
public class ValuationAgent implements Agent<ValuationAgentOutput> {

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
        FactSheetAgentOutput factSheetOutput = (FactSheetAgentOutput) ctx.getPriorOutputs().get(AgentType.FACT_SHEET);
        FinancialFactSheetData factSheet = factSheetOutput.factSheet();

        Map<String, Object> promptData = Map.of("derived_metrics",
                factSheet.derivedMetrics(),
                "share_price_signals",
                factSheet.sharePriceSignals());

        String promptDataJson;
        try {
            promptDataJson = objectMapper.writeValueAsString(promptData);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }

        String prompt = promptRegistry.getPrompt(PromptType.VALUATION).replace("{{ fact_sheet_json }}", promptDataJson);

        String userPrompt = String.format(
                "Analyze the valuation for %s as of %s based on the provided financial fact sheet.",
                ctx.getTicker(),
                ctx.getAsOf());

        String fullPrompt = prompt + "\n" + userPrompt;

        LlmResponse<ValuationAgentOutput> response = llmRouterApi.executeCall(LlmTierType.MINI,
                fullPrompt,
                ValuationAgentOutput.class);

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}