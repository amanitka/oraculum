package com.oraculum.analyst.agents;

import com.oraculum.analyst.agents.base.Agent;
import com.oraculum.analyst.agents.base.AgentOutput;
import com.oraculum.analyst.agents.context.AgentContext;
import com.oraculum.analyst.agents.models.FactSheetOutput;
import com.oraculum.analyst.agents.models.FinancialFactSheetData;
import com.oraculum.analyst.agents.models.FundamentalsOutput;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.AgentType;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class FundamentalsAgent implements Agent<FundamentalsOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return AgentType.FUNDAMENTALS.getAgentName();
    }

    @Override
    public Class<FundamentalsOutput> getOutputModel() {
        return FundamentalsOutput.class;
    }

    @Override
    public AgentOutput<FundamentalsOutput> run(AgentContext ctx) {
        FactSheetOutput factSheetOutput = (FactSheetOutput) ctx.getPriorOutputs().get(AgentType.FACT_SHEET);
        FinancialFactSheetData factSheet = factSheetOutput.factSheet();

        Map<String, Object> promptData = Map.of("income_statement_history",
                factSheet.getIncomeStatementHistory(),
                "balance_sheet_history",
                factSheet.getBalanceSheetHistory(),
                "derived_metrics",
                factSheet.getDerivedMetrics());

        String promptDataJson;
        try {
            promptDataJson = objectMapper.writeValueAsString(promptData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String prompt = promptRegistry.getPrompt(PromptType.FUNDAMENTALS)
                .replace("{{ fact_sheet_json }}", promptDataJson);

        String userPrompt = String.format(
                "Analyze fundamentals for %s as of %s based on the provided financial fact sheet.",
                ctx.getTicker(),
                ctx.getAsOf());

        String fullPrompt = prompt + "\n" + userPrompt;

        LlmResponse<FundamentalsOutput> response = llmRouterApi.executeCall(LlmTierType.MINI,
                fullPrompt,
                FundamentalsOutput.class);

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}