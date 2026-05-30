package com.oraculum.llm.api.dto;

public record LlmResponse<T>(T result,
                             LlmMetrics metrics) {

    public Integer getTotalTokens() {
        return metrics != null && metrics.totalTokens() != null ? metrics.totalTokens() : 0;
    }
}
