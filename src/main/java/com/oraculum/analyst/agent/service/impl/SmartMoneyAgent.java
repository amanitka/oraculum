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
public class SmartMoneyAgent implements Agent<StandardAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;

    @Override
    public AgentType getName() {
        return AgentType.SMART_MONEY;
    }

    private AgentOutput<StandardAgentOutput> getDefaultOutput() {
        return new AgentOutput<>(StandardAgentOutput.empty(
                "No institutional 13F holding data available.",
                "No institutional 13F holding data available for this company."
        ), 0);
    }

    private String getPrompt(AgentContext ctx, String instFlowHistory, String sectorRotation) {
        String focus = ctx.analysisFocus() != null ? ctx.analysisFocus() : "Standard institutional ownership and sector flow analysis.";
        String sector = ctx.company().sectorName() != null ? ctx.company().sectorName() : "Unknown";
        return promptRegistry.getPrompt(PromptType.SMART_MONEY)
                .replace("{{ analysis_focus }}", focus)
                .replace("{{ company_sector }}", sector)
                .replace("{{ all_sectors_rotation }}", sectorRotation)
                .replace("{{ institutional_flow_history }}", instFlowHistory)
                .replace("{{ ticker }}", ctx.ticker())
                .replace("{{ analysis_date }}", ctx.analysisDate().toString());
    }

    @Override
    public AgentOutput<StandardAgentOutput> run(AgentContext ctx) {
        log.info("SmartMoneyAgent starting analysis for ticker: {}", ctx.ticker());
        String instFlowHistory = ctx.factSheetData().getInstitutionalFlowHistoryJson();

        if ("[]".equals(instFlowHistory)) {
            log.warn("No institutional 13F flow history found for ticker: {}. Skipping analysis step.", ctx.ticker());
            return getDefaultOutput();
        }

        String sectorRotation = ctx.factSheetData().getSectorRotationSummaryJson();
        String prompt = getPrompt(ctx, instFlowHistory, sectorRotation);

        LlmResponse<StandardAgentOutput> response = llmRouterApi.executeCall(
                LlmCallRequest.of(
                        LlmTierType.STANDARD,
                        prompt,
                        StandardAgentOutput.class,
                        ctx.correlationId(),
                        CorrelationType.COMPANY_ANALYSIS,
                        getName().name())
        );

        log.info("SmartMoneyAgent successfully generated summary for ticker: {}", ctx.ticker());
        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}
