package com.oraculum.analyst.service;

import com.oraculum.analyst.domain.AnalysisStatus;
import com.oraculum.analyst.domain.CompanyAnalysisEntity;
import com.oraculum.analyst.dto.CompanyAnalysisRequest;
import com.oraculum.analyst.dto.CompanyAnalysisResult;
import com.oraculum.analyst.listener.message.AnalyzeCompanyRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public void executeAnalysis(AnalyzeCompanyRequest request) {
        log.info("Handling orchestrated company analysis request for {}", request.ticker());
        CompanyAnalysisEntity companyAnalysis = initAnalysis(request);
        try {
            markAsRunning(companyAnalysis);
            CompanyAnalysisResult analysisResult = runAnalysisWorkflow(request);
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

    private CompanyAnalysisEntity initAnalysis(AnalyzeCompanyRequest request) {
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
        } catch (Exception e) {
            log.error("Failed to serialize analysis trace for {}: {}", entity.getTicker(), e.getMessage());
            markAsFailed(entity, "Failed to serialize agent trace: " + e.getMessage());
        }
    }

    private CompanyAnalysisResult runAnalysisWorkflow(AnalyzeCompanyRequest request) {
        return workflow.run(new CompanyAnalysisRequest(request.ticker(),
                request.market(),
                request.analysisDate(),
                request.statementVariant()), request.correlationId());
    }
}
