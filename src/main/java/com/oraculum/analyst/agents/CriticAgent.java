package com.oraculum.analyst.agents;

import com.oraculum.analyst.agents.base.Agent;
import com.oraculum.analyst.agents.base.AgentOutput;
import com.oraculum.analyst.agents.context.AgentContext;
import com.oraculum.analyst.agents.models.CriticAgentOutput;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.AgentType;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CriticAgent implements Agent<CriticAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public String getName() {
        return AgentType.CRITIC.getAgentName();
    }

    @Override
    public Class<CriticAgentOutput> getOutputModel() {
        return CriticAgentOutput.class;
    }

    @Override
    public AgentOutput<CriticAgentOutput> run(AgentContext ctx) {
        Map<AgentType, Object> specialistOutputs = ctx.getPriorOutputs()
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey() != AgentType.FACT_SHEET)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        String priorOutputsJson;
        try {
            priorOutputsJson = objectMapper.writeValueAsString(specialistOutputs);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }

        String prompt = promptRegistry.getPrompt(PromptType.CRITIC).replace("{{ prior_outputs }}", priorOutputsJson);

        String userPrompt = String.format(
                "Critique the analysis for %s. Identify any contradictions between the provided agent summaries.",
                ctx.getTicker());

        String fullPrompt = prompt + "\n" + userPrompt;

        LlmResponse<CriticAgentOutput> response = llmRouterApi.executeCall(LlmTierType.PRO, fullPrompt, CriticAgentOutput.class);

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}