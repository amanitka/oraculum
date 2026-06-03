package com.oraculum.analyst.listener;

import com.oraculum.analyst.listener.message.AnalyzeCompanyRequest;
import com.oraculum.analyst.service.CompanyAnalysisOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyAnalysisRequestListener {

    private final CompanyAnalysisOrchestrationService orchestrationService;

    @KafkaListener(topics = "${oraculum.kafka.topics.analyst-request}", groupId = "${oraculum.kafka.consumer-group}")
    public void handleAnalyzeCompanyRequest(AnalyzeCompanyRequest request) {
        orchestrationService.executeAnalysis(request);
    }

    /*
    private CompanyAnalysisEntity createCompanyAnalysisEntity(AnalyzeCompanyRequest request) {
        CompanyAnalysisEntity entity = new CompanyAnalysisEntity();
        entity.setId(request.correlationId());
        entity.setCompanyId(request.companyId());
        entity.setTicker(request.ticker());
        entity.setMarket(request.market());
        entity.setAnalysisDate(request.analysisDate() != null ? request.analysisDate() : LocalDate.now());
        entity.setStatus(AnalysisStatus.PENDING);
        return companyAnalysisService.createOrUpdateAnalysis(entity);
    }

    private void markCompanyAnalysisRecordAsRunning(CompanyAnalysisEntity companyAnalysis) {
        companyAnalysis.setStatus(AnalysisStatus.RUNNING);
        companyAnalysisService.createOrUpdateAnalysis(companyAnalysis);
    }

    private void markCompanyAnalysisRecordAsFailed(CompanyAnalysisEntity companyAnalysis, String error) {
        companyAnalysis.setStatus(AnalysisStatus.FAILED);
        companyAnalysis.setError(error);
        companyAnalysisService.createOrUpdateAnalysis(companyAnalysis);
    }

    private void completeCompanyAnalysisRecord(CompanyAnalysisEntity companyAnalysis,
                                               CompanyAnalysisResultDto analysisResult) {
        companyAnalysis.setStatus(analysisResult.status());
        companyAnalysis.setReport(analysisResult.reportMd());
        companyAnalysis.setOutlook(analysisResult.outlook());
        companyAnalysis.setRecommendation(analysisResult.recommendation());
        companyAnalysis.setConviction(analysisResult.conviction());
        companyAnalysis.setAnalysisData(objectMapper.writeValueAsString(analysisResult.agentTrace()));
        companyAnalysis.setError(analysisResult.error());
        companyAnalysisService.createOrUpdateAnalysis(companyAnalysis);
    }

    private CompanyAnalysisResultDto runAnalysis(AnalyzeCompanyRequest request) {
        return workflow.run(new CompanyAnalysisRequest(request.ticker(),
                request.market(),
                request.analysisDate(),
                request.statementVariant()), request.correlationId());
    }

    @KafkaListener(topics = "${oraculum.kafka.topics.analyst-request}", groupId = "${oraculum.kafka.consumer-group}")
    @Transactional
    public void handleAnalyzeCompanyRequest(AnalyzeCompanyRequest request) {
        log.info("Handling company analysis request for {}", request.ticker());
        CompanyAnalysisEntity companyAnalysis = createCompanyAnalysisEntity(request);
        try {
            markCompanyAnalysisRecordAsRunning(companyAnalysis);
            CompanyAnalysisResultDto analysisResult = runAnalysis(request);
            completeCompanyAnalysisRecord(companyAnalysis, analysisResult);
            log.info("Successfully completed analysis for ticker {} in market {}", request.ticker(), request.market());
        } catch (Exception e) {
            log.error("Analysis failed for for ticker {} in market {}: {}",
                    request.ticker(),
                    request.market(),
                    e.getMessage(),
                    e);
            markCompanyAnalysisRecordAsFailed(companyAnalysis, e.getMessage());
        }
    }
    */
}