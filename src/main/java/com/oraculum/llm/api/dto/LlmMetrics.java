package com.oraculum.llm.api.dto;

public record LlmMetrics(LlmProviderType provider,
                         String model,
                         Integer promptTokens,
                         Integer completionTokens,
                         Integer totalTokens,
                         Long latencyMs) {
}
