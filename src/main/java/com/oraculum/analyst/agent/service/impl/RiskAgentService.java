package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.RiskAgentOutput;
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
public class RiskAgentService implements AgentService<RiskAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;

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

        String prompt = promptRegistry.getPrompt(PromptType.RISK)
                .replace("{{ balance_sheet_history }}", factSheet.getBalanceSheetHistory(ctx.statementVariant()))
                .replace("{{ company_financial_ratios }}", factSheet.getCompanyFinancialRatios(ctx.statementVariant()))
                .replace("{{ daily_share_price_signals }}", factSheet.getDailySharePriceSignals());

        String userPrompt = String.format("Analyze risk for %s as of %s based on the provided financial fact sheet.",
                ctx.ticker(),
                ctx.analysisDate());

        String fullPrompt = prompt + "\n" + userPrompt;

        LlmResponse<RiskAgentOutput> response = llmRouterApi.executeCall(LlmTierType.MINI,
                fullPrompt,
                RiskAgentOutput.class);

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}