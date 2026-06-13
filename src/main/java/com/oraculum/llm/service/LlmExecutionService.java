package com.oraculum.llm.service;

import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.domain.LlmRequest;
import java.util.concurrent.CompletableFuture;

public interface LlmExecutionService {
    <T> CompletableFuture<LlmResponse<T>> executeCall(LlmRequest<T> request);
}
