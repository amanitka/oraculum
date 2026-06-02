package com.oraculum.analyst.agents;

import com.oraculum.analyst.agents.base.Agent;
import com.oraculum.analyst.agents.base.AgentOutput;
import com.oraculum.analyst.agents.context.AgentContext;
import com.oraculum.analyst.agents.models.FactSheetAgentOutput;
import com.oraculum.analyst.agents.models.FinancialFactSheetData;
import com.oraculum.analyst.agents.models.FundamentalsAgentOutput;
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
public class FundamentalsAgent implements Agent<FundamentalsAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public AgentType getName() {
        return AgentType.FUNDAMENTALS;
    }

    @Override
    public Class<FundamentalsAgentOutput> getOutputModel() {
        return FundamentalsAgentOutput.class;
    }

    @Override
    public AgentOutput<FundamentalsAgentOutput> run(AgentContext ctx) {
        FactSheetAgentOutput factSheetOutput = (FactSheetAgentOutput) ctx.priorOutputs().get(AgentType.FACT_SHEET);
        FinancialFactSheetData factSheet = factSheetOutput.factSheet();

        Map<String, Object> promptData = Map.of("income_statement_history",
                factSheet.incomeStatementHistory(),
                "balance_sheet_history",
                factSheet.balanceSheetHistory(),
                "derived_metrics",
                factSheet.derivedMetrics());

        String promptDataJson;
        try {
            promptDataJson = objectMapper.writeValueAsString(promptData);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }

        String prompt = promptRegistry.getPrompt(PromptType.FUNDAMENTALS)
                .replace("{{ fact_sheet_json }}", promptDataJson);

        String userPrompt = String.format(
                "Analyze fundamentals for %s as of %s based on the provided financial fact sheet.",
                ctx.ticker(),
                ctx.runDateTime());

        String fullPrompt = prompt + "\n" + userPrompt;

        LlmResponse<FundamentalsAgentOutput> response = llmRouterApi.executeCall(LlmTierType.MINI,
                fullPrompt,
                FundamentalsAgentOutput.class);

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}