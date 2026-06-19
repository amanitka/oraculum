package com.oraculum.llm.api;

import com.oraculum.llm.api.dto.LlmResponse;

public interface LlmRouterApi {
    <T> LlmResponse<T> executeCall(LlmCallRequest<T> request);
}
