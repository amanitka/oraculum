package com.oraculum.ui.service;

import com.oraculum.analyst.api.dto.CompanyAnalysisRequest;
import com.oraculum.analyst.api.dto.CompanyAnalysisRequestEvent;
import com.oraculum.analyst.api.AnalysisUsageApi;
import com.oraculum.user.api.dto.OraculumUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisRequestService {

    private final ApplicationEventPublisher eventPublisher;
    private final AnalysisUsageApi analysisUsageApi;
    private final com.oraculum.user.api.CurrentUserApi currentUserApi;

    private CompanyAnalysisRequestEvent buildRequestEvent(CompanyAnalysisRequest request, Long userId) {
        return new CompanyAnalysisRequestEvent(
                request.correlationId(),
                request.companyId(),
                request.ticker(),
                request.analysisDate(),
                request.analysisFocus(),
                userId
        );
    }

    public void requestAnalysis(CompanyAnalysisRequest request) {
        OraculumUserDetails userDetails = currentUserApi.getCurrentUserOrThrow();
        Long userId = userDetails.getId();
        analysisUsageApi.checkLimit(userId, userDetails.getLimit());
        CompanyAnalysisRequestEvent eventToPublish = buildRequestEvent(request, userId);

        try {
            eventPublisher.publishEvent(eventToPublish);
            log.info("Published analysis request [{}] by user {}", eventToPublish, userId);
        } catch (Exception e) {
            log.error("Failed to publish analysis request [{}]", eventToPublish, e);
            throw new RuntimeException("Failed to publish analysis request: " + e.getMessage(), e);
        }
    }
}
