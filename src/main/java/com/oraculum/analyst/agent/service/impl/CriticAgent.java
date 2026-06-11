package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.CriticAgentOutput;
import com.oraculum.analyst.agent.service.Agent;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.analyst.util.JsonUtils;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class CriticAgent implements Agent<CriticAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public AgentType getName() {
        return AgentType.CRITIC;
    }

    @Override
    public Class<CriticAgentOutput> getOutputModel() {
        return CriticAgentOutput.class;
    }

    @Override
    public AgentOutput<CriticAgentOutput> run(AgentContext ctx) {
        Map<AgentType, Object> specialistOutputs = ctx.getSpecialistAgentOutputs();
        String priorOutputsJson = JsonUtils.toJson(objectMapper, specialistOutputs, "{}");

        String prompt = promptRegistry.getPrompt(PromptType.CRITIC)
                .replace("{{ analysis_focus }}", ctx.analysisFocus() != null ? ctx.analysisFocus() : "Standard comprehensive analysis.")
                .replace("{{ algorithmic_baseline }}", ctx.factSheetData().getAlgorithmicBaselineJson())
                .replace("{{ prior_outputs }}", priorOutputsJson);

        String userPrompt = String.format(
                "Critique the analysis for %s. Identify any contradictions between the provided agent summaries.",
                ctx.ticker());

        String fullPrompt = prompt + "\n" + userPrompt;

        LlmResponse<CriticAgentOutput> response = llmRouterApi.executeCall(LlmTierType.PRO,
                fullPrompt,
                CriticAgentOutput.class);

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}