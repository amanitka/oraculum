package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.*;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;

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

import com.oraculum.company.api.domain.StatementVariant;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CriticAgent implements Agent<CriticAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;
    private final JsonMapper jsonMapper;

    @Override
    public AgentType getName() {
        return AgentType.CRITIC;
    }


    @Override
    public AgentOutput<CriticAgentOutput> run(AgentContext ctx) {
        Map<AgentType, Object> specialistOutputs = ctx.state().getSpecialistOutputs();
        Map<String, Set<StatementVariant>> agentTimeframes = Arrays.stream(AgentType.values())
                .filter(AgentType::isSpecialist)
                .filter(a -> !a.requiredVariants().isEmpty())
                .collect(Collectors.toMap(AgentType::getAgentName, AgentType::requiredVariants));
        String agentTimeframesJson = JsonUtils.toJson(jsonMapper, agentTimeframes, "{}");
        String priorOutputsJson = JsonUtils.toJson(jsonMapper, specialistOutputs, "{}");

        String fullPrompt = promptRegistry.getPrompt(PromptType.CRITIC)
                .replace("{{ analysis_focus }}", ctx.analysisFocus() != null ? ctx.analysisFocus() : "Standard comprehensive analysis.")
                .replace("{{ agent_timeframes }}", agentTimeframesJson)
                .replace("{{ prior_outputs }}", priorOutputsJson)
                .replace("{{ ticker }}", ctx.ticker());

        LlmResponse<CriticAgentOutput> response = llmRouterApi.executeCall(
                LlmCallRequest.of(LlmTierType.PRO, fullPrompt, CriticAgentOutput.class, ctx.correlationId(), CorrelationType.COMPANY_ANALYSIS, getName().name()));

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}
