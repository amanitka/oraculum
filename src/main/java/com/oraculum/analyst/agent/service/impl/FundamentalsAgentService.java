package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.FundamentalsAgentOutput;
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
public class FundamentalsAgentService implements AgentService<FundamentalsAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;

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
        CompanyFactSheetData factSheet = ctx.factSheetData();

        String prompt = promptRegistry.getPrompt(PromptType.FUNDAMENTALS)
                .replace("{{ income_statement_history }}", factSheet.getIncomeStatementHistory(ctx.statementVariant()))
                .replace("{{ balance_sheet_history }}", factSheet.getBalanceSheetHistory(ctx.statementVariant()))
                .replace("{{ company_financial_ratios }}", factSheet.getCompanyFinancialRatios(ctx.statementVariant()));

        String userPrompt = String.format(
                "Analyze fundamentals for %s as of %s based on the provided financial fact sheet.",
                ctx.ticker(),
                ctx.requestDate());

        String fullPrompt = prompt + "\n" + userPrompt;

        LlmResponse<FundamentalsAgentOutput> response = llmRouterApi.executeCall(LlmTierType.MINI,
                fullPrompt,
                FundamentalsAgentOutput.class);

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}