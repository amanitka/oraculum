package com.oraculum.ui.api;

import com.oraculum.analyst.api.domain.AgentType;

import java.util.UUID;
import java.util.function.Consumer;

public interface CompanyAnalysisProgressBroadcasterService {

    Runnable register(Consumer<ProgressUpdate> listener);

    void broadcast(UUID analysisId, AgentType agentType, boolean isDone);

    record ProgressUpdate(UUID analysisId, AgentType agentType, boolean isDone) {
    }
}
