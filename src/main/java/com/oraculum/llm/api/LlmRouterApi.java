package com.oraculum.llm.api;

import com.oraculum.llm.api.dto.LlmTierType;

public interface LlmRouterApi {
    <T> T generate(LlmTierType tier, String prompt, Class<T> responseType);
}
