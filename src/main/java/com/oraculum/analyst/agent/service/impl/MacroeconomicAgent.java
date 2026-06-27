package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.MacroeconomicAgentOutput;
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
public class MacroeconomicAgent implements Agent<MacroeconomicAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;

    @Override
    public AgentType getName() {
        return AgentType.MACROECONOMIC;
    }

    private AgentOutput<MacroeconomicAgentOutput> getDefaultOutput() {
        return new AgentOutput<>(new MacroeconomicAgentOutput(
                "No specific macroeconomic headwinds or tailwinds were identified for this company's profile."
        ), 0);
    }

    private String getPrompt(AgentContext ctx, String macroeconomicSummary) {
        return promptRegistry.getPrompt(PromptType.MACROECONOMIC)
                .replace("{{ analysis_focus }}", ctx.analysisFocus())
                .replace("{{ ticker }}", ctx.ticker())
                .replace("{{ company_profile }}", ctx.factSheetData().getCompanyProfile())
                .replace("{{ macroeconomic_summary }}", macroeconomicSummary);
    }

    @Override
    public AgentOutput<MacroeconomicAgentOutput> run(AgentContext ctx) {
        log.info("MacroeconomicAgent starting analysis for ticker: {}", ctx.ticker());
        String macroeconomicSummary = ctx.factSheetData().getMacroeconomicSummary();
        if ("[]".equals(macroeconomicSummary)) {
            log.warn("No macroeconomic data found for ticker: {}", ctx.ticker());
            return getDefaultOutput();
        }

        String prompt = getPrompt(ctx, macroeconomicSummary);
        LlmResponse<MacroeconomicAgentOutput> response = llmRouterApi.executeCall(
                LlmCallRequest.of(
                        LlmTierType.STANDARD,
                        prompt,
                        MacroeconomicAgentOutput.class,
                        ctx.correlationId(),
                        CorrelationType.COMPANY_ANALYSIS,
                        getName().name())
        );

        log.info("MacroeconomicAgent successfully generated summary for ticker: {}", ctx.ticker());
        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}
