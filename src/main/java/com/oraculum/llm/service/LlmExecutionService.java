package com.oraculum.llm.service;

import com.oraculum.llm.domain.LlmRequest;

public interface LlmExecutionService {
    <T> T executeCall(LlmRequest<T> request);
}
