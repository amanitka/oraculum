package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.*;
import com.oraculum.analyst.agent.service.Agent;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.api.domain.FinancialDataProfile;
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
    public AgentOutput<RiskAgentOutput> run(AgentContext ctx) {
        CompanyFactSheetData factSheet = ctx.factSheetData();

        SharePriceAgentOutput sharePriceOutput = (SharePriceAgentOutput) ctx.state().getAgentOutput(AgentType.SHARE_PRICE);
        String sharePriceJson = JsonUtils.toJson(objectMapper, sharePriceOutput, "{}");

        FundamentalsAgentOutput fundamentalsOutput = (FundamentalsAgentOutput) ctx.state().getAgentOutput(AgentType.FUNDAMENTALS);
        String fundamentalsJson = JsonUtils.toJson(objectMapper, fundamentalsOutput, "{}");

        CashFlowAgentOutput cashFlowOutput = (CashFlowAgentOutput) ctx.state().getAgentOutput(AgentType.CASH_FLOW);
        String cashFlowJson = JsonUtils.toJson(objectMapper, cashFlowOutput, "{}");

        FinancialDataProfile profile = getName().getDataProfile();

        String prompt = promptRegistry.getPrompt(PromptType.RISK)
                .replace("{{ analysis_focus }}", ctx.analysisFocus() != null ? ctx.analysisFocus() : "Standard comprehensive analysis.")
                .replace("{{ balance_sheet_history_a }}", factSheet.getBalanceSheetHistory(StatementVariant.ANNUAL, profile.periodLimit(StatementVariant.ANNUAL)))
                .replace("{{ company_financial_ratios_a }}", factSheet.getCompanyFinancialRatios(StatementVariant.ANNUAL, profile.periodLimit(StatementVariant.ANNUAL)))
                .replace("{{ balance_sheet_history_ttm }}", factSheet.getBalanceSheetHistory(StatementVariant.TTM, profile.periodLimit(StatementVariant.TTM)))
                .replace("{{ company_financial_ratios_ttm }}", factSheet.getCompanyFinancialRatios(StatementVariant.TTM, profile.periodLimit(StatementVariant.TTM)))
                .replace("{{ share_price_analysis }}", sharePriceJson)
                .replace("{{ fundamentals_analysis }}", fundamentalsJson)
                .replace("{{ cash_flow_analysis }}", cashFlowJson)
                .replace("{{ ticker }}", ctx.ticker())
                .replace("{{ analysis_date }}", ctx.analysisDate().toString());

        String fullPrompt = appendCriticFeedbackIfPresent(prompt, ctx);

        LlmResponse<RiskAgentOutput> response = llmRouterApi.executeCall(LlmTierType.STANDARD,
                fullPrompt,
                RiskAgentOutput.class);

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}