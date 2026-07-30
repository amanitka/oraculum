package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.*;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;

import com.oraculum.analyst.agent.service.Agent;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.company.api.dto.SharePriceSignalDto;
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
public class EarningsEstimatesAgent implements Agent<StandardAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;

    @Override
    public AgentType getName() {
        return AgentType.EARNINGS_ESTIMATES;
    }

    private String preparePrompt(AgentContext ctx, String earningsEstimatesJson) {
        SharePriceSignalDto currentSignal = ctx.getLatestSignal();

        String priceStr = currentSignal != null && currentSignal.sharePrice() != null ? String.valueOf(currentSignal.sharePrice()) : "N/A";
        String peStr = currentSignal != null && currentSignal.peRatio() != null ? String.valueOf(currentSignal.peRatio()) : "N/A";
        String epsGrowthStr = currentSignal != null && currentSignal.epsYoyGrowth() != null
                ? String.format("%.4f (= %.2f%% — this is a DECIMAL, not a percentage)",
                currentSignal.epsYoyGrowth(), currentSignal.epsYoyGrowth() * 100)
                : "N/A";

        String prompt = promptRegistry.getPrompt(PromptType.EARNINGS_ESTIMATES)
                .replace("{{ analysis_focus }}", ctx.analysisFocus() != null ? ctx.analysisFocus() : "Standard comprehensive analysis.")
                .replace("{{ earnings_estimates_json }}", earningsEstimatesJson)
                .replace("{{ ticker }}", ctx.ticker())
                .replace("{{ current_price }}", priceStr)
                .replace("{{ trailing_pe }}", peStr)
                .replace("{{ historical_eps_growth }}", epsGrowthStr)
                .replace("{{ canonical_facts }}", ctx.factSheetData().getCanonicalFacts())
                .replace("{{ recent_sec_ex99_1_summaries }}", ctx.factSheetData().getRecentSecEx991Summaries());

        return appendCriticFeedbackIfPresent(prompt, ctx);
    }

    @Override
    public AgentOutput<StandardAgentOutput> run(AgentContext ctx) {
        log.info("EarningsEstimatesAgent starting analysis for ticker: {}", ctx.ticker());
        var factSheet = ctx.factSheetData();
        String earningsEstimatesJson = factSheet.getFutureEarningsEstimates(ctx.analysisDate());
        if ("[]".equals(earningsEstimatesJson)) {
            return new AgentOutput<>(new StandardAgentOutput("No earnings estimates available.", 0.0, 0.0, java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.Map.of(), "No earnings estimates available."), 0);
        }
        String fullPrompt = preparePrompt(ctx, earningsEstimatesJson);
        LlmResponse<StandardAgentOutput> response = llmRouterApi.executeCall(
                LlmCallRequest.of(LlmTierType.STANDARD, fullPrompt, StandardAgentOutput.class, ctx.correlationId(), CorrelationType.COMPANY_ANALYSIS, getName().name()));

        log.info("EarningsEstimatesAgent successfully generated summary for ticker: {}", ctx.ticker());
        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }

}
