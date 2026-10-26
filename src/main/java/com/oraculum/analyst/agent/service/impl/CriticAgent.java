package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.CriticAgentOutput;
import com.oraculum.analyst.agent.dto.CriticAgentOutput.Contradiction;
import com.oraculum.analyst.agent.dto.CriticAgentOutput.Resolution;
import com.oraculum.analyst.agent.dto.CriticAgentOutput.RerunInstruction;
import com.oraculum.analyst.agent.service.Agent;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.analyst.service.PreCriticValidationService;
import com.oraculum.analyst.util.JsonUtils;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.llm.api.LlmCallRequest;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.CorrelationType;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CriticAgent implements Agent<CriticAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;
    private final JsonMapper jsonMapper;
    private final PreCriticValidationService preCriticValidationService;

    @Override
    public AgentType getName() {
        return AgentType.CRITIC;
    }

    @Override
    public AgentOutput<CriticAgentOutput> run(AgentContext ctx) {
        List<Contradiction> deterministicChecks = preCriticValidationService.validate(ctx);
        String prompt = buildPrompt(ctx, deterministicChecks);

        LlmResponse<CriticAgentOutput> response = llmRouterApi.executeCall(
                LlmCallRequest.of(LlmTierType.PRO, prompt, CriticAgentOutput.class,
                        ctx.correlationId(), CorrelationType.COMPANY_ANALYSIS, getName().name()));

        CriticAgentOutput mergedResult = mergeFindings(response.result(), deterministicChecks);
        int tokens = response.metrics() != null ? response.metrics().totalTokens() : 0;
        return new AgentOutput<>(mergedResult, tokens);
    }

    private String buildPrompt(AgentContext ctx, List<Contradiction> deterministicChecks) {
        Map<AgentType, Object> specialistOutputs = ctx.state().getSpecialistOutputs();
        Map<String, Set<StatementVariant>> agentTimeframes = Arrays.stream(AgentType.values())
                .filter(AgentType::isSpecialist)
                .filter(a -> !a.requiredVariants().isEmpty())
                .collect(Collectors.toMap(AgentType::getAgentName, AgentType::requiredVariants));

        String agentTimeframesJson = JsonUtils.toJson(jsonMapper, agentTimeframes, "{}");
        String priorOutputsJson = JsonUtils.toJson(jsonMapper, specialistOutputs, "{}");
        String deterministicJson = JsonUtils.toJson(jsonMapper, deterministicChecks, "[]");
        String focus = ctx.analysisFocus() != null ? ctx.analysisFocus() : "Standard comprehensive analysis.";

        return promptRegistry.getPrompt(PromptType.CRITIC)
                .replace("{{ analysis_focus }}", focus)
                .replace("{{ agent_timeframes }}", agentTimeframesJson)
                .replace("{{ prior_outputs }}", priorOutputsJson)
                .replace("{{ deterministic_checks }}", deterministicJson)
                .replace("{{ ticker }}", ctx.ticker() != null ? ctx.ticker() : "");
    }

    private CriticAgentOutput mergeFindings(CriticAgentOutput output, List<Contradiction> deterministic) {
        List<Contradiction> combined = new ArrayList<>(deterministic);
        if (output != null && output.contradictionsFound() != null) {
            for (Contradiction c : output.contradictionsFound()) {
                if (!isDuplicate(c, combined)) {
                    combined.add(c);
                }
            }
        }
        List<RerunInstruction> reruns = output != null && output.recommendedReruns() != null
                ? new ArrayList<>(output.recommendedReruns()) : new ArrayList<>();
        appendMissingReruns(combined, reruns);

        boolean consistent = !hasUnreconciledContradictions(combined) && reruns.isEmpty();
        String counterThesis = output != null ? output.counterThesis() : null;
        return new CriticAgentOutput(combined, consistent, reruns, counterThesis);
    }

    private boolean hasUnreconciledContradictions(List<Contradiction> contradictions) {
        return contradictions.stream()
                .anyMatch(c -> c.resolution() != Resolution.NARRATIVE_RECONCILED);
    }

    private boolean isDuplicate(Contradiction candidate, List<Contradiction> existing) {
        return existing.stream().anyMatch(e ->
                e.correctionType() == candidate.correctionType() &&
                        e.agentsInvolved().equals(candidate.agentsInvolved()));
    }

    private void appendMissingReruns(List<Contradiction> contradictions, List<RerunInstruction> reruns) {
        for (Contradiction c : contradictions) {
            if (c.resolution() == Resolution.RERUN_APPLIED) {
                AgentType agent = resolveSpecialist(c);
                boolean exists = reruns.stream().anyMatch(r -> r.specialist() == agent);
                if (!exists && agent != null) {
                    reruns.add(new RerunInstruction(agent, 1, c.description()));
                }
            }
        }
    }

    private AgentType resolveSpecialist(Contradiction c) {
        if (c.agentsInvolved() == null || c.agentsInvolved().isEmpty()) {
            return null;
        }
        try {
            return AgentType.valueOf(c.agentsInvolved().getFirst().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
