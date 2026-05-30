package com.oraculum.llm.domain;

import org.springframework.ai.chat.client.ChatClient;

public record LlmRequest<T>(ChatClient client,
                            String prompt,
                            LlmProviderType provider,
                            String model,
                            Double temperature,
                            Class<T> responseType) {
}
