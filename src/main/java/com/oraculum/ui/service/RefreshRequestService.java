package com.oraculum.ui.service;

import com.oraculum.common.config.OraculumProperties;
import com.oraculum.ui.request.HarvesterRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RefreshRequestService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public RefreshRequestService(KafkaTemplate<String, Object> kafkaTemplate, OraculumProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = properties.kafka().topics().harvesterRequest();
    }

    public void publish(HarvesterRequest request) {
        try {
            kafkaTemplate.send(topic, request.getCorrelationId().toString(), request);
            log.info("Published {} [{}] to {}", request.getRequestType(), request.getCorrelationId(), topic);
        } catch (Exception e) {
            log.error("Failed to publish {} request", request.getRequestType(), e);
            throw new RuntimeException("Failed to publish request: " + e.getMessage(), e);
        }
    }
}
