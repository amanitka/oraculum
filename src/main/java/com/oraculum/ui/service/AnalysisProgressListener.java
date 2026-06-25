package com.oraculum.ui.service;

import com.oraculum.analyst.api.event.CompanyAnalysisProgressEvent;
import com.oraculum.ui.api.AnalysisProgressBroadcasterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisProgressListener {

    private final AnalysisProgressBroadcasterService broadcaster;

    @ApplicationModuleListener
    public void onAnalysisProgressEvent(CompanyAnalysisProgressEvent event) {
        log.debug("Received analysis progress event: {}", event);
        broadcaster.broadcast(event.correlationId(), event.agentType(), event.isComplete());
    }
}
