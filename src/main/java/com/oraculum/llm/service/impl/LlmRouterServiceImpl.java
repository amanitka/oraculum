package com.oraculum.llm.service.impl;

import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import com.oraculum.llm.config.LlmProperties;
import com.oraculum.llm.domain.LlmProviderType;
import com.oraculum.llm.domain.LlmRequest;
import com.oraculum.llm.service.LlmExecutionService;
import com.oraculum.llm.service.LlmRouterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmRouterServiceImpl implements LlmRouterService {

    private final Map<LlmProviderType, ChatClient> chatClients;
    private final LlmExecutionService executionService;
    private final LlmHealthProvider health;
    private final LlmProperties properties;

    @Override
    public <T> LlmResponse<T> executeCall(LlmTierType tier, String prompt, Class<T> type) {
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
                var result = executionService.executeCall(new LlmRequest<>(client,
                        prompt,
                        provider,
                        model,
                        properties.common().temperature(),
                        properties.common().maxCompletionTokens(),
                        type)).join();
                health.markSuccess(provider);
                return result;
            } catch (Exception e) {
                log.warn("LLM call failed for provider: {} [model: {}]. Error: {}. Falling back to next available provider.",
                        provider,
                        model,
                        e.getMessage());
                last = e;
                health.markFailure(provider, 30_000);
            }
        }

        throw new RuntimeException("All providers failed", last);
    }

    private String resolveModel(LlmTierType tier, LlmProviderType provider) {
        Map<LlmProviderType, String> providerMap = properties.models().get(tier);
        if (providerMap == null) {
            String message = String.format("Configuration missing for tier: %s", tier);
            log.error(message);
            throw new IllegalArgumentException(message);
        }
        String model = providerMap.get(provider);
        if (model == null) {
            String message = String.format("Model configuration missing for tier: %s and provider: %s", tier, provider);
            log.error(message);
            throw new IllegalArgumentException(message);
        }
        return model;
    }
}