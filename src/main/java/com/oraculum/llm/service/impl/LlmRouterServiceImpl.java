package com.oraculum.llm.service.impl;

import com.oraculum.llm.api.dto.LlmTierType;
import com.oraculum.llm.domain.LlmProviderType;
import com.oraculum.llm.domain.LlmRequest;
import com.oraculum.llm.property.LlmProperties;
import com.oraculum.llm.service.LlmExecutionService;
import com.oraculum.llm.service.LlmRouterService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class LlmRouterServiceImpl implements LlmRouterService {
    private final Map<LlmProviderType, ChatClient> chatClients;
    private final LlmExecutionService executionService;
    private final LlmHealthProvider health;
    private final LlmProperties properties;

    @Override
    public <T> T generate(LlmTierType tier, String prompt, Class<T> type) {
        Exception last = null;
        for (LlmProviderType provider : properties.common().providerFallbackOrder()) {
            if (health.isBlocked(provider)) {
                continue;
            }
            ChatClient client = chatClients.get(provider);
            if (client == null) {
                continue;
            }
            String model = resolveModel(tier, provider);
            try {
                T result = executionService.executeCall(new LlmRequest<>(client, prompt, model, properties.common()
                        .temperature(), type));
                health.markSuccess(provider);
                return result;
            } catch (Exception e) {
                last = e;
                health.markFailure(provider, 30_000);
            }
        }

        throw new RuntimeException("All providers failed", last);
    }

    private String resolveModel(LlmTierType tier, LlmProviderType provider) {
        return properties.models().get(tier).get(provider);
    }
}
