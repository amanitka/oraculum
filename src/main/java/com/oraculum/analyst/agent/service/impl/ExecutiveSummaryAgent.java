package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.*;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;

import com.oraculum.analyst.agent.service.Agent;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.api.dto.ExecutiveSummaryAgentOutput;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.llm.api.LlmCallRequest;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.CorrelationType;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ExecutiveSummaryAgent implements Agent<ExecutiveSummaryAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;

    @Override
    public AgentType getName() {
        return AgentType.EXECUTIVE_SUMMARY;
    }

    @Override
    public AgentOutput<ExecutiveSummaryAgentOutput> run(AgentContext ctx) {
        SynthesizerAgentOutput synthOutput = (SynthesizerAgentOutput) ctx.state().getAgentOutput(AgentType.SYNTHESIZER);
        if (synthOutput == null) {
            throw new IllegalStateException("SynthesizerAgentOutput must be present before running ExecutiveSummaryAgent");
        }

        String prompt = buildPrompt(ctx, synthOutput);

        LlmResponse<ExecutiveSummaryAgentOutput> response = llmRouterApi.executeCall(
                LlmCallRequest.of(LlmTierType.PRO, prompt, ExecutiveSummaryAgentOutput.class,
                        ctx.correlationId(), CorrelationType.COMPANY_ANALYSIS, getName().name())
        );

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }

    private String buildPrompt(AgentContext ctx, SynthesizerAgentOutput synthOutput) {
        List<String> keyDrivers = synthOutput.keyDrivers() != null ? synthOutput.keyDrivers() : List.of();
        List<String> keyRisks = synthOutput.keyRisks() != null ? synthOutput.keyRisks() : List.of();

        return promptRegistry.getPrompt(PromptType.EXECUTIVE_SUMMARY)
                .replace("{{ report }}", synthOutput.executiveSummary() != null ? synthOutput.executiveSummary() : "")
                .replace("{{ outlook }}", synthOutput.outlook() != null ? synthOutput.outlook().name() : "")
                .replace("{{ recommendation }}", synthOutput.recommendation() != null ? synthOutput.recommendation().name() : "")
                .replace("{{ conviction }}", String.valueOf(synthOutput.conviction()))
                .replace("{{ key_drivers }}", String.join("\n- ", keyDrivers))
                .replace("{{ key_risks }}", String.join("\n- ", keyRisks))
                .replace("{{ ticker }}", ctx.ticker());
    }
}
