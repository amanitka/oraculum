package com.oraculum.analyst.service;

import com.oraculum.analyst.api.domain.AnalysisStatus;
import com.oraculum.analyst.api.dto.CompanyAnalysisRequestEvent;
import com.oraculum.analyst.api.event.CompanyAnalysisProgressEvent;
import com.oraculum.analyst.domain.CompanyAnalysisEntity;
import com.oraculum.analyst.dto.CompanyAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyAnalysisOrchestrationService {

    private final CompanyAnalysisService companyAnalysisService;
    private final CompanyAnalysisWorkflowService workflow;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public void executeAnalysis(CompanyAnalysisRequestEvent request) {
        log.info("Handling orchestrated company analysis request for {}", request.ticker());
        if (companyAnalysisService.isAnalysisCompleted(request.correlationId())) {
            log.info("Analysis already completed for ticker {}, correlation id {}", request.ticker(), request.correlationId());
            return;
        }
        CompanyAnalysisEntity companyAnalysis = initAnalysis(request);
        try {
            markAsRunning(companyAnalysis);
            CompanyAnalysisResult analysisResult = workflow.run(request);
            completeAnalysis(companyAnalysis, analysisResult);
            log.info("Successfully completed analysis for ticker {} in market {}", request.ticker(), request.market());
        } catch (Exception e) {
            log.error("Analysis failed for ticker {} in market {}: {}",
                    request.ticker(),
                    request.market(),
                    e.getMessage(),
                    e);
            markAsFailed(companyAnalysis, e.getMessage());
        }
    }

    private CompanyAnalysisEntity initAnalysis(CompanyAnalysisRequestEvent request) {
        CompanyAnalysisEntity entity = new CompanyAnalysisEntity();
        entity.setId(request.correlationId());
        entity.setCompanyId(request.companyId());
        entity.setTicker(request.ticker());
        entity.setMarket(request.market());
        entity.setAnalysisDate(request.analysisDate() != null ? request.analysisDate() : LocalDate.now());
        entity.setStatus(AnalysisStatus.PENDING);
        return companyAnalysisService.createOrUpdateAnalysis(entity);
    }

    private void markAsRunning(CompanyAnalysisEntity entity) {
        entity.setStatus(AnalysisStatus.RUNNING);
        companyAnalysisService.createOrUpdateAnalysis(entity);
    }

    private void markAsFailed(CompanyAnalysisEntity entity, String error) {
        entity.setStatus(AnalysisStatus.FAILED);
        entity.setError(error);
        companyAnalysisService.createOrUpdateAnalysis(entity);
        eventPublisher.publishEvent(new CompanyAnalysisProgressEvent(entity.getId(), null, true));
    }

    private void completeAnalysis(CompanyAnalysisEntity entity, CompanyAnalysisResult result) {
        try {
            entity.setStatus(result.status());
            entity.setReport(result.reportMd());
            entity.setOutlook(result.outlook());
            entity.setRecommendation(result.recommendation());
            entity.setConviction(result.conviction());
            entity.setAnalysisData(objectMapper.writeValueAsString(result.agentTrace()));
            entity.setError(result.error());
            companyAnalysisService.createOrUpdateAnalysis(entity);
            eventPublisher.publishEvent(new CompanyAnalysisProgressEvent(entity.getId(), null, true));
        } catch (Exception e) {
            log.error("Failed to serialize analysis trace for {}: {}", entity.getTicker(), e.getMessage());
            markAsFailed(entity, "Failed to serialize agent trace: " + e.getMessage());
        }
    }
}
