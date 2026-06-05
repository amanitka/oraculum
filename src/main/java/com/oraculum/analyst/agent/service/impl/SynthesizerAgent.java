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
        Map<AgentType, Object> specialistOutputs = ctx.priorOutputs()
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey() != AgentType.CRITIC)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        String specialistOutputJSon = JsonUtils.toJson(objectMapper, specialistOutputs, "{}");

        CriticAgentOutput criticOutput = (CriticAgentOutput) ctx.priorOutputs().get(AgentType.CRITIC);
        String criticOutputJson = JsonUtils.toJson(objectMapper, criticOutput, "{}");

        String prompt = promptRegistry.getPrompt(PromptType.SYNTHESIZER)
                .replace("{{ specialist_outputs }}", specialistOutputJSon)
                .replace("{{ critic_output }}", criticOutputJson);

        String userPrompt = String.format("Synthesize the analysis for %s. Generate the final report and " +
                        "structured verdict, explicitly addressing the critic's findings.",
                ctx.ticker());

        String fullPrompt = prompt + "\n" + userPrompt;

        LlmResponse<SynthesizerAgentOutput> response = llmRouterApi.executeCall(LlmTierType.PRO,
                fullPrompt,
                SynthesizerAgentOutput.class);

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}