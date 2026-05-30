package com.oraculum.llm.service;

import com.oraculum.llm.dto.LlmRequest;

public interface LlmExecutionService {
    <T> T executeCall(LlmRequest<T> request);
}
