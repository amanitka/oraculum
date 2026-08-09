package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.CriticAgentOutput;
import com.oraculum.analyst.agent.service.Agent;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.api.dto.AnalysisResult;
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
public class SynthesizerAgent implements Agent<AnalysisResult> {

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
    public AgentOutput<AnalysisResult> run(AgentContext ctx) {
        Map<AgentType, Object> specialistOutputs = ctx.state().getSpecialistOutputs();
        String specialistOutputJson = JsonUtils.toJson(jsonMapper, specialistOutputs, "{}");
        CriticAgentOutput criticOutput = (CriticAgentOutput) ctx.state().getAgentOutput(AgentType.CRITIC);
        String criticOutputJson = JsonUtils.toJson(jsonMapper, criticOutput, "{}");

        String prompt = promptRegistry.getPrompt(PromptType.SYNTHESIZER)
                .replace("{{ analysis_focus }}", ctx.analysisFocus() != null ? ctx.analysisFocus() : "Standard comprehensive analysis.")
                .replace("{{ company_profile }}", ctx.factSheetData() != null && ctx.factSheetData().getCompanyProfile() != null ? ctx.factSheetData().getCompanyProfile() : "{}")
                .replace("{{ specialist_output }}", specialistOutputJson != null ? specialistOutputJson : "{}")
                .replace("{{ critic_output }}", criticOutputJson != null ? criticOutputJson : "{}")
                .replace("{{ canonical_facts }}", ctx.factSheetData() != null && ctx.factSheetData().getCanonicalFacts() != null ? ctx.factSheetData().getCanonicalFacts() : "{}")
                .replace("{{ unaddressed_warning }}", getWarningMessage(criticOutput))
                .replace("{{ ticker }}", ctx.ticker() != null ? ctx.ticker() : "");

        LlmResponse<AnalysisResult> response = llmRouterApi.executeCall(
                LlmCallRequest.of(LlmTierType.PRO, prompt, AnalysisResult.class, ctx.correlationId(), CorrelationType.COMPANY_ANALYSIS, getName().name()));

        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}
