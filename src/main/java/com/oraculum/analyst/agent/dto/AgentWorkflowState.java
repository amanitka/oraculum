package com.oraculum.analyst.agent.dto;

import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.company.api.domain.StatementVariant;
import lombok.Getter;
import lombok.Setter;

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
public class AgentWorkflowState {

    private final Map<AgentType, Object> agentOutputs = new EnumMap<>(AgentType.class);
    private final Map<String, Object> agentTrace = new java.util.LinkedHashMap<>();
    private int totalTokens = 0;

    private String analysisFocus;
    private Map<AgentType, StatementVariant> statementVariants;
    private Map<AgentType, String> criticFeedback;

    public void addTokens(int tokens) {
        this.totalTokens += tokens;
    }

    public void putAgentTrace(String key, Object output) {
        agentTrace.put(key, output);
    }

    public void putAgentOutput(AgentType type, Object output) {
        agentOutputs.put(type, output);
    }

    public Object getAgentOutput(AgentType type) {
        return agentOutputs.get(type);
    }

    public Map<AgentType, Object> getSpecialistOutputs() {
        return agentOutputs.entrySet().stream()
                .filter(e -> e.getKey().isSpecialist())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public String getCriticFeedbackFor(AgentType type) {
        return criticFeedback != null ? criticFeedback.get(type) : null;
    }

    public void clearCriticFeedback() {
        this.criticFeedback = null;
    }
}
