package com.oraculum.llm.domain;

import com.oraculum.llm.api.LlmCallRequest;
import com.oraculum.llm.api.dto.LlmProviderType;

public record LlmExecutionContext<T>(
        LlmCallRequest<T> request,
        LlmProviderType provider,
        String model,
        boolean useHealthCheck
) {
}