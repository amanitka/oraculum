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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        if (criticOutput == null) return "";
        List<CriticAgentOutput.Contradiction> unresolved = criticOutput.unresolvedContradictions();
        if (unresolved.isEmpty()) return "";
        String items = unresolved.stream()
                .map(CriticAgentOutput.Contradiction::description)
                .collect(Collectors.joining("; "));
        return "\nWARNING: The following contradictions could not be resolved — " +
                "hedge these explicitly in your report: " + items;
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
                .replace("{{ canonical_facts }}", ctx.factSheetData().getCanonicalFacts())
                .replace("{{ unaddressed_warning }}", getWarningMessage(criticOutput))
                .replace("{{ ticker }}", ctx.ticker());

        LlmResponse<SynthesizerAgentOutput> response = llmRouterApi.executeCall(
                LlmCallRequest.of(LlmTierType.PRO, prompt, SynthesizerAgentOutput.class, ctx.correlationId(), CorrelationType.COMPANY_ANALYSIS, getName().name()));

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}
