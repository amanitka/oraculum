package com.oraculum.analyst.agents;

import com.oraculum.analyst.agents.base.Agent;
import com.oraculum.analyst.agents.base.AgentOutput;
import com.oraculum.analyst.agents.context.AgentContext;
import com.oraculum.analyst.agents.models.CriticAgentOutput;
import com.oraculum.analyst.agents.models.SynthesizerAgentOutput;
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
        Map<AgentType, Object> specialistOutputs = ctx.getPriorOutputs()
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey() != AgentType.FACT_SHEET && entry.getKey() != AgentType.CRITIC)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        String priorOutputsJson;
        try {
            priorOutputsJson = objectMapper.writeValueAsString(specialistOutputs);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }

        CriticAgentOutput criticOutput = (CriticAgentOutput) ctx.getPriorOutputs().get(AgentType.CRITIC);
        String criticReportJson;
        try {
            criticReportJson = criticOutput != null ? objectMapper.writeValueAsString(criticOutput) : "{}";
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }

        String prompt = promptRegistry.getPrompt(PromptType.SYNTHESIZER)
                .replace("{{ prior_outputs }}", priorOutputsJson)
                .replace("{{ critic_report }}", criticReportJson);

        String userPrompt = String.format(
                "Synthesize the analysis for %s. The resolved statement template was '%s'. The default variant was " +
                        "'%s'. Generate the final report and structured verdict, explicitly addressing the critic's " +
                        "findings.",
                ctx.getTicker(),
                ctx.getTemplate(),
                ctx.getDefaultVariant());

        String fullPrompt = prompt + "\n" + userPrompt;

        LlmResponse<SynthesizerAgentOutput> response = llmRouterApi.executeCall(LlmTierType.PRO,
                fullPrompt,
                SynthesizerAgentOutput.class);

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}