package com.oraculum.analyst.listener;

import com.oraculum.analyst.api.dto.CompanyAnalysisRequestEvent;
import com.oraculum.analyst.service.CompanyAnalysisOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyAnalysisRequestListener {

    private final CompanyAnalysisOrchestrationService orchestrationService;

    @Async("analysisExecutor")
    @EventListener
    public void handleCompanyAnalysisRequestedEvent(CompanyAnalysisRequestEvent event) {
        try {
            orchestrationService.executeAnalysis(event);
        } catch (Exception e) {
            log.error("Failed to execute analysis request asynchronously: {}", e.getMessage(), e);
        }
    }
}
