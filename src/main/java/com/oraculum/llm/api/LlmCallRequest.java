package com.oraculum.llm.api;

import com.oraculum.llm.api.dto.CorrelationType;
import com.oraculum.llm.api.dto.LlmTierType;
import java.util.UUID;

public record LlmCallRequest<T>(
    LlmTierType tier,
    String prompt,
    Class<T> responseType,
    UUID correlationId,
    CorrelationType correlationType,
    String source
) {
    public static <T> LlmCallRequest<T> of(LlmTierType tier, String prompt, Class<T> responseType) {
        return new LlmCallRequest<>(tier, prompt, responseType, null, null, null);
    }

    public static <T> LlmCallRequest<T> of(LlmTierType tier, String prompt, Class<T> responseType, UUID correlationId, CorrelationType correlationType, String source) {
        return new LlmCallRequest<>(tier, prompt, responseType, correlationId, correlationType, source);
    }
}
