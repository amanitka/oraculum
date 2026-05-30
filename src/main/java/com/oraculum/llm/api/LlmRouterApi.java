package com.oraculum.llm.api;

import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;

public interface LlmRouterApi {
    <T> LlmResponse<T> executeCall(LlmTierType tier, String prompt, Class<T> responseType);
}
