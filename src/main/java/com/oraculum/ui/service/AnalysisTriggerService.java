package com.oraculum.ui.service;

import com.oraculum.analyst.api.dto.CompanyAnalysisRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class AnalysisTriggerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AnalysisTriggerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void triggerAnalysis(CompanyAnalysisRequest request) {
        kafkaTemplate.send("oraculum.analysis.request", request);
    }
}
