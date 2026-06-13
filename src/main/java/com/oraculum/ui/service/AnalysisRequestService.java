package com.oraculum.ui.service;

import com.oraculum.analyst.api.dto.CompanyAnalysisRequestEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisRequestService {

    private final ApplicationEventPublisher eventPublisher;

    public void requestAnalysis(CompanyAnalysisRequestEvent request) {
        try {
            eventPublisher.publishEvent(request);
            log.info("Published analysis request [{}]", request);
        } catch (Exception e) {
            log.error("Failed to publish analysis request [{}]", request, e);
            throw new RuntimeException("Failed to publish analysis request: " + e.getMessage(), e);
        }
    }
}
