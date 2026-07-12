package com.oraculum.llm.service.impl;

import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import com.oraculum.llm.config.LlmProperties;
import com.oraculum.llm.api.LlmCallRequest;
import com.oraculum.llm.api.LlmExecutionEvent;
import com.oraculum.llm.api.dto.LlmProviderType;
import com.oraculum.llm.domain.LlmRequest;
import com.oraculum.llm.service.LlmExecutionService;
import com.oraculum.llm.service.LlmRouterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmRouterServiceImpl implements LlmRouterService {

    private final Map<LlmProviderType, ChatClient> chatClients;
    private final LlmExecutionService executionService;
    private final LlmHealthProvider health;
    private final LlmProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public <T> LlmResponse<T> executeCall(LlmCallRequest<T> request) {
        List<LlmProviderType> fallbackOrder = resolveFallbackOrder(request);
        Exception last = null;
        for (LlmProviderType provider : fallbackOrder) {
            try {
                LlmResponse<T> result = tryExecuteProvider(provider, request);
                if (result != null) {
                    return result;
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                last = e;
            }
        }
        throw new RuntimeException("All providers failed", last);
    }

    private List<LlmProviderType> resolveFallbackOrder(LlmCallRequest<?> request) {
        return (request.providerFallbackOrderOverride() != null
                && !request.providerFallbackOrderOverride().isEmpty())
                ? request.providerFallbackOrderOverride()
                : properties.common().providerFallbackOrder();
    }

    private <T> LlmResponse<T> tryExecuteProvider(LlmProviderType provider, LlmCallRequest<T> request) {
        if (health.isBlocked(provider)) {
            return null;
        }
        ChatClient client = chatClients.get(provider);
        if (client == null) {
            return null;
        }
        String model = resolveModel(request.tier(), provider);
        if (model == null) {
            return null;
        }
        try {
            LlmResponse<T> result = executionService.executeCall(new LlmRequest<>(
                    client,
                    request.prompt(),
                    provider,
                    model,
                    properties.common().temperature(),
                    properties.common().maxCompletionTokens(),
                    request.responseType()
            )).join();
            health.markSuccess(provider);
            publishEvent(request, result);
            return result;
        } catch (Exception e) {
            log.warn("LLM call failed for provider: {} [model: {}]. Error: {}. Falling back to next available provider.",
                    provider, model, e.getMessage());
            health.markFailure(provider);
            throw e;
        }
    }

    private <T> void publishEvent(LlmCallRequest<T> request, LlmResponse<T> result) {
        try {
            eventPublisher.publishEvent(new LlmExecutionEvent(
                    request.correlationId(),
                    request.correlationType(),
                    request.source(),
                    result.metrics(),
                    request.prompt(),
                    result.result()
            ));
        } catch (Exception ex) {
            log.warn("Failed to publish LLM execution event", ex);
        }
    }

    private String resolveModel(LlmTierType tier, LlmProviderType provider) {
        Map<LlmProviderType, String> providerMap = properties.models().get(tier);
        if (providerMap == null) {
            String message = String.format("Configuration missing for tier: %s", tier);
            log.error(message);
            throw new IllegalArgumentException(message);
        }
        return providerMap.get(provider);
    }
}
