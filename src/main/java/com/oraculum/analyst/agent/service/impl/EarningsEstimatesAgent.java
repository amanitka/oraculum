package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.EarningsEstimatesAgentOutput;
import com.oraculum.analyst.agent.service.Agent;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.company.api.dto.SharePriceSignalDto;
import com.oraculum.harvester.api.HarvesterLiveApi;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EarningsEstimatesAgent implements Agent<EarningsEstimatesAgentOutput> {

    private final HarvesterLiveApi harvesterLiveApi;
    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;

    @Override
    public AgentType getName() {
        return AgentType.EARNINGS_ESTIMATES;
    }

    private String preparePrompt(AgentContext ctx, String earningsEstimates) {
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
                .replace("{{ earnings_estimates_json }}", earningsEstimates)
                .replace("{{ ticker }}", ctx.ticker())
                .replace("{{ current_price }}", priceStr)
                .replace("{{ trailing_pe }}", peStr)
                .replace("{{ historical_eps_growth }}", epsGrowthStr);

        return appendCriticFeedbackIfPresent(prompt, ctx);
    }

    @Override
    public AgentOutput<EarningsEstimatesAgentOutput> run(AgentContext ctx) {
        log.info("EarningsEstimatesAgent starting analysis for ticker: {}", ctx.ticker());
        Optional<String> earningsEstimatesOpt = harvesterLiveApi.fetchEarningsEstimates(ctx.ticker());
        if (earningsEstimatesOpt.isEmpty() || earningsEstimatesOpt.get().isBlank()) {
            log.warn("Earnings estimates data unavailable for ticker: {}. Skipping LLM call.", ctx.ticker());
            String skipMessage = "Earnings estimates data unavailable: API quota exhausted (or reserved capacity reached). " +
                    "This analysis does not include forward-looking EPS/revenue consensus estimates.";
            return new AgentOutput<>(new EarningsEstimatesAgentOutput(skipMessage), 0);
        }
        String prompt = preparePrompt(ctx, earningsEstimatesOpt.get());
        LlmResponse<EarningsEstimatesAgentOutput> response = llmRouterApi.executeCall(
                LlmTierType.STANDARD,
                prompt,
                EarningsEstimatesAgentOutput.class
        );

        log.info("EarningsEstimatesAgent successfully generated summary for ticker: {}", ctx.ticker());
        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}
