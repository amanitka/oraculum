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
    public AgentOutput<CriticAgentOutput> run(AgentContext ctx) {
        Map<AgentType, Object> specialistOutputs = ctx.state().getSpecialistOutputs();
        java.util.Map<String, java.util.Set<com.oraculum.company.api.domain.StatementVariant>> agentTimeframes = java.util.Arrays.stream(AgentType.values())
                .filter(AgentType::isSpecialist)
                .filter(a -> !a.getRequiredVariants().isEmpty())
                .collect(java.util.stream.Collectors.toMap(AgentType::getAgentName, AgentType::getRequiredVariants));
        String agentTimeframesJson = JsonUtils.toJson(objectMapper, agentTimeframes, "{}");
        String priorOutputsJson = JsonUtils.toJson(objectMapper, specialistOutputs, "{}");

        String prompt = promptRegistry.getPrompt(PromptType.CRITIC)
                .replace("{{ analysis_focus }}", ctx.analysisFocus() != null ? ctx.analysisFocus() : "Standard comprehensive analysis.")
                .replace("{{ agent_timeframes }}", agentTimeframesJson)
                .replace("{{ prior_outputs }}", priorOutputsJson)
                .replace("{{ ticker }}", ctx.ticker());

        LlmResponse<CriticAgentOutput> response = llmRouterApi.executeCall(LlmTierType.PRO,
                prompt,
                CriticAgentOutput.class);

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}