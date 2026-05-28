package com.oraculum.llm.api.dto;

public record LlmResponseDto(
        String text,
        String model,
        int inputTokens,
        int outputTokens,
        long latencyMs,
        String finishReason
) {
}
