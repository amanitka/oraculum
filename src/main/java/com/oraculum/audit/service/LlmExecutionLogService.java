package com.oraculum.audit.service;

import com.oraculum.audit.domain.LlmExecutionLogEntity;
import com.oraculum.audit.repository.LlmExecutionLogRepository;
import com.oraculum.llm.api.LlmExecutionEvent;
import com.oraculum.llm.api.dto.LlmMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LlmExecutionLogService {

    private final LlmExecutionLogRepository repository;
    private final ObjectMapper objectMapper;

    public void createLog(LlmExecutionEvent event) {
        LlmMetrics metrics = event.metrics();
        LlmExecutionLogEntity entity = new LlmExecutionLogEntity();
        entity.setCorrelationId(event.correlationId());
        entity.setCorrelationType(event.correlationType() != null ? event.correlationType().name() : null);
        entity.setSource(event.source());
        entity.setProviderCode(metrics != null && metrics.provider() != null ? metrics.provider().name().toUpperCase(Locale.ROOT) : null);
        entity.setModelName(metrics != null && metrics.model() != null ? metrics.model() : null);
        entity.setInputTokens(metrics != null ? metrics.promptTokens() : null);
        entity.setOutputTokens(metrics != null ? metrics.completionTokens() : null);
        entity.setTotalTokens(metrics != null ? metrics.totalTokens() : null);
        entity.setPrompt(event.prompt());
        if (event.responseResult() != null) {
            entity.setResponse(objectMapper.writeValueAsString(event.responseResult()));
        }
        repository.save(entity);
    }

}
