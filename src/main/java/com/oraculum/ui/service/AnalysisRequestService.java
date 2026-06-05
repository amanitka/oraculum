package com.oraculum.ui.service;

import com.oraculum.analyst.api.dto.CompanyAnalysisRequest;
import com.oraculum.common.config.OraculumProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AnalysisRequestService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public AnalysisRequestService(KafkaTemplate<String, Object> kafkaTemplate, OraculumProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = properties.kafka().topics().analystRequest();
    }

    public void requestAnalysis(CompanyAnalysisRequest request) {
        try {
            kafkaTemplate.send(topic, request);
            log.info("Published analysis request [{}] to topic {}", request, topic);
        } catch (Exception e) {
            log.error("Failed to publish analysis request [{}]", request, e);
            throw new RuntimeException("Failed to publish analysis request: " + e.getMessage(), e);
        }
    }
}
