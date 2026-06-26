package com.oraculum.ui.service;

import com.oraculum.analyst.api.event.CompanyAnalysisProgressEvent;
import com.oraculum.ui.api.CompanyAnalysisProgressBroadcasterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyAnalysisProgressListener {

    private final CompanyAnalysisProgressBroadcasterService broadcaster;

    @Async
    @EventListener
    public void onAnalysisProgressEvent(CompanyAnalysisProgressEvent event) {
        log.debug("Received analysis progress event: {}", event);
        broadcaster.broadcast(event.correlationId(), event.agentType(), event.isComplete());
    }
}
