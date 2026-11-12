package com.oraculum.llm.service;

import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.domain.LlmRequest;

public interface LlmExecutionService {
    <T> LlmResponse<T> executeCall(LlmRequest<T> request);
}
