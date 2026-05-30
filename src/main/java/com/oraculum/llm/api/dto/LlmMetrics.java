package com.oraculum.llm.api.dto;

import com.oraculum.llm.domain.LlmProviderType;

public record LlmMetrics(LlmProviderType provider,
                         String model,
                         Integer promptTokens,
                         Integer completionTokens,
                         Integer totalTokens,
                         Long latencyMs) {
}