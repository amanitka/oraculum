package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.CriticAgentOutput;
import com.oraculum.analyst.agent.dto.SynthesizerAgentOutput;
import com.oraculum.analyst.agent.service.Agent;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.AgentType;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.analyst.util.JsonUtils;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SynthesizerAgent implements Agent<SynthesizerAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public AgentType getName() {
        return AgentType.SYNTHESIZER;
    }

    @Override
    public Class<SynthesizerAgentOutput> getOutputModel() {
        return SynthesizerAgentOutput.class;
    }

    @Override
    public AgentOutput<SynthesizerAgentOutput> run(AgentContext ctx) {
        // Safe selection: Only include specialists and ignore null outputs to prevent NPE in Collectors.toMap
        Map<AgentType, Object> specialistOutputs = ctx.agentOutputs()
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().isSpecialist() && entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        String specialistOutputJson = JsonUtils.toJson(objectMapper, specialistOutputs, "{}");

        // Retrieve Critic Agent output from prior outputs
        CriticAgentOutput criticOutput = (CriticAgentOutput) ctx.agentOutputs().get(AgentType.CRITIC);
        String criticOutputJson = JsonUtils.toJson(objectMapper, criticOutput, "{}");

        String prompt = promptRegistry.getPrompt(PromptType.SYNTHESIZER)
                .replace("{{ specialist_output }}", specialistOutputJson)
                .replace("{{ critic_output }}", criticOutputJson);

        String userPrompt = String.format("Synthesize the analysis for %s. Generate the final report and " +
                        "structured verdict, explicitly resolving the contradictions flagged in the critic's report.",
                ctx.ticker());

        String fullPrompt = prompt + "\n" + userPrompt;

        LlmResponse<SynthesizerAgentOutput> response = llmRouterApi.executeCall(LlmTierType.PRO,
                fullPrompt,
                SynthesizerAgentOutput.class);

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}