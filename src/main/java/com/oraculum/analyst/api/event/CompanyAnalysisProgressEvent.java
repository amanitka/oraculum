package com.oraculum.analyst.api.event;

import com.oraculum.analyst.api.domain.AgentType;

import java.util.UUID;

public record CompanyAnalysisProgressEvent(
        UUID correlationId,
        AgentType agentType,
        boolean isComplete
) {
}
