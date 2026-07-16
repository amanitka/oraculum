package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.FundamentalsAgentOutput;
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
public class FundamentalsAgent implements Agent<FundamentalsAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;

    @Override
    public AgentType getName() {
        return AgentType.FUNDAMENTALS;
    }


    @Override
    public AgentOutput<FundamentalsAgentOutput> run(AgentContext ctx) {
        CompanyFactSheetData factSheet = ctx.factSheetData();
        FinancialDataProfile profile = getName().getDataProfile();
        String prompt = promptRegistry.getPrompt(PromptType.FUNDAMENTALS)
                .replace("{{ analysis_focus }}", ctx.analysisFocus() != null ? ctx.analysisFocus() : "Standard comprehensive analysis.")
                // Quarterly: recent sequential trend (all periods)
                .replace("{{ income_statement_history_q }}", factSheet.getIncomeStatementHistory(StatementVariant.QUARTERLY))
                .replace("{{ balance_sheet_history_q }}", factSheet.getBalanceSheetHistory(StatementVariant.QUARTERLY))
                .replace("{{ company_financial_ratios_q }}", factSheet.getCompanyFinancialRatios(StatementVariant.QUARTERLY))
                // Annual: limited to profile-defined periods — income + ratios only, no balance sheet
                .replace("{{ income_statement_history_a }}", factSheet.getIncomeStatementHistory(StatementVariant.ANNUAL, profile.periodLimit(StatementVariant.ANNUAL)))
                .replace("{{ company_financial_ratios_a }}", factSheet.getCompanyFinancialRatios(StatementVariant.ANNUAL, profile.periodLimit(StatementVariant.ANNUAL)))
                .replace("{{ industry_ratios }}", factSheet.getLatestIndustryRatios(StatementVariant.TTM))
                .replace("{{ sec_mda_summaries }}", factSheet.getRecentSecMdSummaries())
                .replace("{{ ticker }}", ctx.ticker())
                .replace("{{ analysis_date }}", ctx.analysisDate().toString());

        String fullPrompt = appendCriticFeedbackIfPresent(prompt, ctx);

        LlmResponse<FundamentalsAgentOutput> response = llmRouterApi.executeCall(
                LlmCallRequest.of(LlmTierType.STANDARD, fullPrompt, FundamentalsAgentOutput.class, ctx.correlationId(), CorrelationType.COMPANY_ANALYSIS, getName().name()));

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}
