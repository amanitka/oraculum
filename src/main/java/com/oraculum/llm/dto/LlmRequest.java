package com.oraculum.llm.dto;

import org.springframework.ai.chat.client.ChatClient;

public record LlmRequest<T>(ChatClient client, String prompt, String model, Double temperature, Class<T> responseType) {
}
