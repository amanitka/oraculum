package com.oraculum.llm.domain;

import com.oraculum.llm.api.dto.LlmProviderType;
import org.springframework.ai.chat.client.ChatClient;

public record LlmRequest<T>(ChatClient client,
                            String prompt,
                            LlmProviderType provider,
                            String model,
                            Double temperature,
                            Integer maxCompletionTokens,
                            Class<T> responseType) {
}
