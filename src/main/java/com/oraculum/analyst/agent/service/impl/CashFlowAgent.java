package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.CashFlowAgentOutput;
import com.oraculum.analyst.agent.dto.FundamentalsAgentOutput;
import com.oraculum.analyst.agent.service.Agent;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.api.domain.FinancialDataProfile;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.analyst.util.JsonUtils;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.llm.api.LlmCallRequest;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.CorrelationType;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class CashFlowAgent implements Agent<CashFlowAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public AgentType getName() {
        return AgentType.CASH_FLOW;
    }


    @Override
    public AgentOutput<CashFlowAgentOutput> run(AgentContext ctx) {
        CompanyFactSheetData factSheet = ctx.factSheetData();

        FundamentalsAgentOutput fundamentalsOutput = (FundamentalsAgentOutput) ctx.state().getAgentOutput(AgentType.FUNDAMENTALS);
        String fundamentalsJson = JsonUtils.toJson(objectMapper, fundamentalsOutput, "{}");

        FinancialDataProfile profile = getName().getDataProfile();

        String prompt = promptRegistry.getPrompt(PromptType.CASH_FLOW)
                .replace("{{ analysis_focus }}", ctx.analysisFocus() != null ? ctx.analysisFocus() : "Standard comprehensive analysis.")
                .replace("{{ cash_flow_history_ttm }}", factSheet.getCashFlowHistory(StatementVariant.TTM, profile.periodLimit(StatementVariant.TTM)))
                .replace("{{ company_financial_ratios_ttm }}", factSheet.getCompanyFinancialRatios(StatementVariant.TTM, profile.periodLimit(StatementVariant.TTM)))
                .replace("{{ fundamentals_analysis }}", fundamentalsJson)
                .replace("{{ ticker }}", ctx.ticker())
                .replace("{{ analysis_date }}", ctx.analysisDate().toString());

        String fullPrompt = appendCriticFeedbackIfPresent(prompt, ctx);

        LlmResponse<CashFlowAgentOutput> response = llmRouterApi.executeCall(
                LlmCallRequest.of(LlmTierType.STANDARD, fullPrompt, CashFlowAgentOutput.class, ctx.correlationId(), CorrelationType.COMPANY_ANALYSIS, getName().name()));

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}