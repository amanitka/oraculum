package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.ValuationAgentOutput;
import com.oraculum.analyst.agent.service.Agent;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.api.domain.FinancialDataProfile;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.llm.api.LlmCallRequest;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.CorrelationType;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValuationAgent implements Agent<ValuationAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;

    @Override
    public AgentType getName() {
        return AgentType.VALUATION;
    }



    @Override
    public AgentOutput<ValuationAgentOutput> run(AgentContext ctx) {
        CompanyFactSheetData factSheet = ctx.factSheetData();
        FinancialDataProfile profile = getName().getDataProfile();

        String recentDailyJson = factSheet.getLatestDailySharePriceSignals(5);
        String prompt = promptRegistry.getPrompt(PromptType.VALUATION)
                .replace("{{ analysis_focus }}", ctx.analysisFocus() != null ? ctx.analysisFocus() : "Standard comprehensive analysis.")
                .replace("{{ company_financial_ratios_a }}", factSheet.getCompanyFinancialRatios(StatementVariant.ANNUAL, profile.periodLimit(StatementVariant.ANNUAL)))
                .replace("{{ company_financial_ratios_ttm }}", factSheet.getCompanyFinancialRatios(StatementVariant.TTM, profile.periodLimit(StatementVariant.TTM)))
                .replace("{{ industry_ratios }}", factSheet.getLatestTtmIndustryRatios(profile.periodLimit(StatementVariant.TTM)))
                .replace("{{ daily_share_price_signals }}", recentDailyJson)
                .replace("{{ ticker }}", ctx.ticker())
                .replace("{{ analysis_date }}", ctx.analysisDate().toString());

        String fullPrompt = appendCriticFeedbackIfPresent(prompt, ctx);

        LlmResponse<ValuationAgentOutput> response = llmRouterApi.executeCall(
                LlmCallRequest.of(LlmTierType.STANDARD, fullPrompt, ValuationAgentOutput.class, ctx.correlationId(), CorrelationType.COMPANY_ANALYSIS, getName().name()));

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}