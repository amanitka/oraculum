package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.StandardAgentOutput;
import com.oraculum.analyst.agent.service.Agent;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.llm.api.LlmCallRequest;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.CorrelationType;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class InsiderTransactionAgent implements Agent<StandardAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;

    @Override
    public AgentType getName() {
        return AgentType.INSIDER_TRANSACTION;
    }

    private AgentOutput<StandardAgentOutput> getDefaultOutput() {
        return new AgentOutput<>(StandardAgentOutput.empty("No insider transactions available."), 0);
    }

    private String getPrompt(AgentContext ctx, String insiderByPerson) {
        return promptRegistry.getPrompt(PromptType.INSIDER_TRANSACTION)
                .replace("{{ analysis_focus }}", ctx.analysisFocus() != null ? ctx.analysisFocus() : "")
                .replace("{{ insider_summary }}", ctx.factSheetData().getInsiderTransactionSummary())
                .replace("{{ insider_transactions_by_person }}", insiderByPerson)
                .replace("{{ ticker }}", ctx.ticker())
                .replace("{{ analysis_date }}", ctx.analysisDate().toString());
    }

    @Override
    public AgentOutput<StandardAgentOutput> run(AgentContext ctx) {
        log.info("InsiderTransactionAgent starting analysis for ticker: {}", ctx.ticker());
        String insiderByPerson = ctx.factSheetData().getInsiderTransactionsByPerson();
        if ("[]".equals(insiderByPerson)) {
            log.warn("No insider transactions found for ticker: {}", ctx.ticker());
            return getDefaultOutput();
        }

        String prompt = appendCriticFeedbackIfPresent(getPrompt(ctx, insiderByPerson), ctx);
        LlmResponse<StandardAgentOutput> response = llmRouterApi.executeCall(
                LlmCallRequest.of(
                        LlmTierType.STANDARD,
                        prompt,
                        StandardAgentOutput.class,
                        ctx.correlationId(),
                        CorrelationType.COMPANY_ANALYSIS,
                        getName().name())
        );

        log.info("InsiderTransactionAgent successfully generated summary for ticker: {}", ctx.ticker());
        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}
