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
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EarningsEstimatesAgent implements Agent<EarningsEstimatesAgentOutput> {

    private final HarvesterLiveApi harvesterLiveApi;
    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public AgentType getName() {
        return AgentType.EARNINGS_ESTIMATES;
    }

    private String getFutureEstimates(LocalDate analysisDate, List<EarningsEstimateDto> estimates) {
        try {
            List<EarningsEstimateDto> filtered = estimates.stream()
                    .filter(est -> est.date() != null && est.date().isAfter(analysisDate))
                    .collect(Collectors.toList());

            return objectMapper.writeValueAsString(filtered);
        } catch (Exception e) {
            log.warn("Failed to serialize filtered earnings estimates for future dates", e);
            return "[]";
        }
    }

    private String preparePrompt(AgentContext ctx, List<EarningsEstimateDto> earningsEstimates) {
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
                .replace("{{ earnings_estimates_json }}", getFutureEstimates(ctx.analysisDate(), earningsEstimates))
                .replace("{{ ticker }}", ctx.ticker())
                .replace("{{ current_price }}", priceStr)
                .replace("{{ trailing_pe }}", peStr)
                .replace("{{ historical_eps_growth }}", epsGrowthStr);

        return appendCriticFeedbackIfPresent(prompt, ctx);
    }

    @Override
    public AgentOutput<EarningsEstimatesAgentOutput> run(AgentContext ctx) {
        log.info("EarningsEstimatesAgent starting analysis for ticker: {}", ctx.ticker());
        Optional<List<EarningsEstimateDto>> earningsEstimatesOpt = harvesterLiveApi.fetchEarningsEstimates(ctx.ticker());
        if (earningsEstimatesOpt.isEmpty() || earningsEstimatesOpt.get().isEmpty()) {
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
