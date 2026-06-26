package com.oraculum.ui.service;

import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.ui.api.CompanyAnalysisProgressBroadcasterService;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Service
public class CompanyAnalysisProgressBroadcasterServiceImpl implements CompanyAnalysisProgressBroadcasterService {

    private final CopyOnWriteArrayList<Consumer<ProgressUpdate>> listeners = new CopyOnWriteArrayList<>();

    @Override
    public Runnable register(Consumer<ProgressUpdate> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    @Override
    public void broadcast(UUID analysisId, AgentType agentType, boolean isDone) {
        ProgressUpdate update = new ProgressUpdate(analysisId, agentType, isDone);
        for (Consumer<ProgressUpdate> listener : listeners) {
            try {
                listener.accept(update);
            } catch (Exception e) {
                // Ignore listener exceptions to prevent breaking the broadcast loop
            }
        }
    }
}
