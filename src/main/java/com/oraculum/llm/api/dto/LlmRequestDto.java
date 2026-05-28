package com.oraculum.llm.api.dto;

import java.util.List;
import java.util.Map;

public record LlmRequestDto(List<Map<String, Object>> messages, String model, int maxTokens, double temperature,
                            LlmResponseFormatDto responseFormat) {
}
