package com.oraculum.analyst.listener;

import com.oraculum.analyst.api.dto.CompanyAnalysisRequest;
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
    public void executeCompanyAnalysisRequest(CompanyAnalysisRequest request) {
        try {
            orchestrationService.executeAnalysis(request);
        } catch (Exception e) {
            log.error("Failed to execute analysis request: {}", e.getMessage(), e);
        }
    }
}