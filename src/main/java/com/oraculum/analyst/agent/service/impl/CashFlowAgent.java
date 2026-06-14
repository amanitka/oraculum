package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.CashFlowAgentOutput;
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
public class CashFlowAgent implements Agent<CashFlowAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;

    @Override
    public AgentType getName() {
        return AgentType.CASH_FLOW;
    }



    @Override
    public AgentOutput<CashFlowAgentOutput> run(AgentContext ctx) {
        CompanyFactSheetData factSheet = ctx.factSheetData();
        StatementVariant variant = ctx.getVariantFor(getName());

        String prompt = promptRegistry.getPrompt(PromptType.CASH_FLOW)
                .replace("{{ analysis_focus }}", ctx.analysisFocus() != null ? ctx.analysisFocus() : "Standard comprehensive analysis.")
                .replace("{{ cash_flow_history }}", factSheet.getCashFlowHistory(variant))
                .replace("{{ company_financial_ratios }}", factSheet.getCompanyFinancialRatios(variant))
                .replace("{{ ticker }}", ctx.ticker())
                .replace("{{ analysis_date }}", ctx.analysisDate().toString());

        String fullPrompt = appendCriticFeedbackIfPresent(prompt, ctx);

        LlmResponse<CashFlowAgentOutput> response = llmRouterApi.executeCall(LlmTierType.STANDARD,
                fullPrompt,
                CashFlowAgentOutput.class);

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}