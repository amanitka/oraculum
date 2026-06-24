package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.InsiderAgentOutput;
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
public class InsiderAgent implements Agent<InsiderAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;

    @Override
    public AgentType getName() {
        return AgentType.INSIDER;
    }

    @Override
    public AgentOutput<InsiderAgentOutput> run(AgentContext ctx) {
        log.info("InsiderAgent starting analysis for ticker: {}", ctx.ticker());
        String recentTransactions = ctx.factSheetData().getRecentInsiderTransactions();
        String summary = ctx.factSheetData().getInsiderTransactionSummary();

        if (recentTransactions == null || "[]".equals(recentTransactions) || recentTransactions.isBlank()) {
            log.warn("No recent insider transactions found for ticker: {}", ctx.ticker());
            return new AgentOutput<>(new InsiderAgentOutput(
                    com.oraculum.analyst.api.domain.InsiderSentiment.NEUTRAL,
                    3, // Neutral conviction
                    java.util.List.of("No recent transactions"),
                    "No cluster buying observed.",
                    "There are no recent insider transactions to analyze for this ticker."
            ), 0);
        }

        String systemPrompt = promptRegistry.getPrompt(PromptType.INSIDER)
                .replace("{{ analysis_focus }}", ctx.analysisFocus() != null ? ctx.analysisFocus() : "Standard comprehensive analysis.")
                .replace("{{ insider_summary }}", summary != null ? summary : "{}")
                .replace("{{ recent_transactions }}", recentTransactions)
                .replace("{{ ticker }}", ctx.ticker())
                .replace("{{ analysis_date }}", ctx.analysisDate().toString());

        String fullPrompt = appendCriticFeedbackIfPresent(systemPrompt, ctx);

        LlmResponse<InsiderAgentOutput> response = llmRouterApi.executeCall(
                LlmCallRequest.of(LlmTierType.STANDARD, fullPrompt, InsiderAgentOutput.class, ctx.correlationId(), CorrelationType.COMPANY_ANALYSIS, getName().name()));

        log.info("InsiderAgent successfully generated summary for ticker: {}", ctx.ticker());
        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}
