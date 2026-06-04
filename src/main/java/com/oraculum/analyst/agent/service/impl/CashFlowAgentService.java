package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.CashFlowAgentOutput;
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

@Component
@RequiredArgsConstructor
public class CashFlowAgentService implements AgentService<CashFlowAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;

    @Override
    public AgentType getName() {
        return AgentType.CASH_FLOW;
    }

    @Override
    public Class<CashFlowAgentOutput> getOutputModel() {
        return CashFlowAgentOutput.class;
    }

    @Override
    public AgentOutput<CashFlowAgentOutput> run(AgentContext ctx) {
        CompanyFactSheetData factSheet = ctx.factSheetData();

        String prompt = promptRegistry.getPrompt(PromptType.CASH_FLOW)
                .replace("{{ cash_flow_history }}", factSheet.getCashFlowHistory(ctx.statementVariant()))
                .replace("{{ company_financial_ratios }}", factSheet.getCompanyFinancialRatios(ctx.statementVariant()));

        String userPrompt = String.format(
                "Analyze cash flow for %s as of %s based on the provided financial fact sheet.",
                ctx.ticker(),
                ctx.analysisDate());

        String fullPrompt = prompt + "\n" + userPrompt;

        LlmResponse<CashFlowAgentOutput> response = llmRouterApi.executeCall(LlmTierType.MINI,
                fullPrompt,
                CashFlowAgentOutput.class);

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}