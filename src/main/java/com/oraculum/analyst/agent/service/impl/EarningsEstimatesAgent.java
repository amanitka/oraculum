package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.EarningsEstimatesAgentOutput;
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
public class EarningsEstimatesAgent implements Agent<EarningsEstimatesAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;

    @Override
    public AgentType getName() {
        return AgentType.EARNINGS_ESTIMATES;
    }

    private String preparePrompt(AgentContext ctx, String earningsEstimatesJson) {
        SharePriceSignalDto currentSignal = ctx.getLatestSignal();
        String priceStr = "N/A";
        String peStr = "N/A";
        String epsGrowthStr = "N/A";

        if (currentSignal != null) {
            priceStr = currentSignal.sharePrice() != null ? String.valueOf(currentSignal.sharePrice()) : "N/A";
            peStr = currentSignal.peRatio() != null ? String.valueOf(currentSignal.peRatio()) : "N/A";
            epsGrowthStr = currentSignal.epsYoyGrowth() != null ? String.valueOf(currentSignal.epsYoyGrowth()) : "N/A";
        }
        String prompt = promptRegistry.getPrompt(PromptType.EARNINGS_ESTIMATES)
                .replace("{{ analysis_focus }}", ctx.analysisFocus() != null ? ctx.analysisFocus() : "Standard comprehensive analysis.")
                .replace("{{ earnings_estimates_json }}", earningsEstimatesJson)
                .replace("{{ ticker }}", ctx.ticker())
                .replace("{{ current_price }}", priceStr)
                .replace("{{ trailing_pe }}", peStr)
                .replace("{{ historical_eps_growth }}", epsGrowthStr);

        return appendCriticFeedbackIfPresent(prompt, ctx);
    }

    @Override
    public AgentOutput<EarningsEstimatesAgentOutput> run(AgentContext ctx) {
        log.info("EarningsEstimatesAgent starting analysis for ticker: {}", ctx.ticker());
        String earningsEstimatesJson = ctx.factSheetData().getFutureEarningsEstimates(ctx.analysisDate());
        if ("[]".equals(earningsEstimatesJson)) {
            log.warn("Earnings estimates data unavailable for ticker: {}. Skipping LLM call.", ctx.ticker());
            String skipMessage = "Earnings estimates data unavailable: API quota exhausted (or reserved capacity reached). " +
                    "This analysis does not include forward-looking EPS/revenue consensus estimates.";
            return new AgentOutput<>(new EarningsEstimatesAgentOutput(skipMessage), 0);
        }
        String fullPrompt = preparePrompt(ctx, earningsEstimatesJson);
        LlmResponse<EarningsEstimatesAgentOutput> response = llmRouterApi.executeCall(
                LlmCallRequest.of(LlmTierType.STANDARD, fullPrompt, EarningsEstimatesAgentOutput.class, ctx.correlationId(), CorrelationType.COMPANY_ANALYSIS, getName().name()));

        log.info("EarningsEstimatesAgent successfully generated summary for ticker: {}", ctx.ticker());
        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }

}
