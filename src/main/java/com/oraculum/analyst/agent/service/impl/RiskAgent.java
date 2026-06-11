package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.*;
import com.oraculum.analyst.agent.service.Agent;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.analyst.util.JsonUtils;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RiskAgent implements Agent<RiskAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public AgentType getName() {
        return AgentType.RISK;
    }

    @Override
    public Class<RiskAgentOutput> getOutputModel() {
        return RiskAgentOutput.class;
    }

    @Override
    public AgentOutput<RiskAgentOutput> run(AgentContext ctx) {
        CompanyFactSheetData factSheet = ctx.factSheetData();
        StatementVariant variant = ctx.getVariantFor(getName());

        SharePriceAgentOutput sharePriceOutput = (SharePriceAgentOutput) ctx.agentOutputs().get(AgentType.SHARE_PRICE);
        String sharePriceJson = JsonUtils.toJson(objectMapper, sharePriceOutput, "{}");

        FundamentalsAgentOutput fundamentalsOutput = (FundamentalsAgentOutput) ctx.agentOutputs().get(AgentType.FUNDAMENTALS);
        String fundamentalsJson = JsonUtils.toJson(objectMapper, fundamentalsOutput, "{}");

        CashFlowAgentOutput cashFlowOutput = (CashFlowAgentOutput) ctx.agentOutputs().get(AgentType.CASH_FLOW);
        String cashFlowJson = JsonUtils.toJson(objectMapper, cashFlowOutput, "{}");

        String prompt = promptRegistry.getPrompt(PromptType.RISK)
                .replace("{{ analysis_focus }}", ctx.analysisFocus() != null ? ctx.analysisFocus() : "Standard comprehensive analysis.")
                .replace("{{ balance_sheet_history }}", factSheet.getBalanceSheetHistory(variant))
                .replace("{{ company_financial_ratios }}", factSheet.getCompanyFinancialRatios(variant))
                .replace("{{ share_price_analysis }}", sharePriceJson)
                .replace("{{ fundamentals_analysis }}", fundamentalsJson)
                .replace("{{ cash_flow_analysis }}", cashFlowJson);

        String userPrompt = String.format("Analyze risk for %s as of %s based on the provided financial fact sheet.",
                ctx.ticker(),
                ctx.analysisDate());

        String fullPrompt = prompt + "\n" + userPrompt;

        LlmResponse<RiskAgentOutput> response = llmRouterApi.executeCall(LlmTierType.STANDARD,
                fullPrompt,
                RiskAgentOutput.class);

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}