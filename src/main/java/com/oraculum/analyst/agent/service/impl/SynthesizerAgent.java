package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.CriticAgentOutput;
import com.oraculum.analyst.agent.dto.SynthesizerAgentOutput;
import com.oraculum.analyst.agent.service.Agent;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.analyst.util.JsonUtils;
import com.oraculum.llm.api.LlmCallRequest;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.CorrelationType;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SynthesizerAgent implements Agent<SynthesizerAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;
    private final JsonMapper jsonMapper;

    @Override
    public AgentType getName() {
        return AgentType.SYNTHESIZER;
    }

    private String getWarningMessage(CriticAgentOutput criticOutput) {
        if (criticOutput != null && !criticOutput.isConsistent()) {
            return "\nWARNING: The Critic identified inconsistencies that could not be automatically resolved (specialist rerun limits reached). " +
                    "You MUST carefully evaluate these contradictions yourself and provide the final reconciliation in your report.";
        } else {
            return "";
        }
    }

    @Override
    public AgentOutput<SynthesizerAgentOutput> run(AgentContext ctx) {
        // Safe selection: Only include specialists and ignore null outputs to prevent NPE in Collectors.toMap
        Map<AgentType, Object> specialistOutputs = ctx.state().getSpecialistOutputs();
        String specialistOutputJson = JsonUtils.toJson(jsonMapper, specialistOutputs, "{}");
        // Retrieve Critic Agent output from prior outputs
        CriticAgentOutput criticOutput = (CriticAgentOutput) ctx.state().getAgentOutput(AgentType.CRITIC);
        String criticOutputJson = JsonUtils.toJson(jsonMapper, criticOutput, "{}");

        String prompt = promptRegistry.getPrompt(PromptType.SYNTHESIZER)
                .replace("{{ analysis_focus }}", ctx.analysisFocus() != null ? ctx.analysisFocus() : "Standard comprehensive analysis.")
                .replace("{{ company_profile }}", ctx.factSheetData().getCompanyProfile())
                .replace("{{ specialist_output }}", specialistOutputJson)
                .replace("{{ critic_output }}", criticOutputJson)
                .replace("{{ ttm_ratios_ground_truth }}", ctx.factSheetData().getLatestTtmRatios(4))
                .replace("{{ unaddressed_warning }}", getWarningMessage(criticOutput))
                .replace("{{ ticker }}", ctx.ticker());

        LlmResponse<SynthesizerAgentOutput> response = llmRouterApi.executeCall(
                LlmCallRequest.of(LlmTierType.PRO, prompt, SynthesizerAgentOutput.class, ctx.correlationId(), CorrelationType.COMPANY_ANALYSIS, getName().name()));

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}