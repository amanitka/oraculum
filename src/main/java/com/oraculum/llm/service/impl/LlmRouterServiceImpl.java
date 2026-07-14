package com.oraculum.llm.service.impl;

import com.oraculum.llm.api.LlmCallRequest;
import com.oraculum.llm.api.LlmExecutionEvent;
import com.oraculum.llm.api.dto.LlmProviderType;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import com.oraculum.llm.config.LlmProperties;
import com.oraculum.llm.domain.LlmExecutionContext;
import com.oraculum.llm.domain.LlmRequest;
import com.oraculum.llm.exception.LlmExecuteException;
import com.oraculum.llm.exception.LlmMissingTierConfigurationException;
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
        boolean isSingle = fallbackOrder.size() == 1;
        Exception lastException = null;

        for (LlmProviderType provider : fallbackOrder) {
            String model = resolveModel(request.tier(), provider);
            if (model == null) {
                continue;
            }
            LlmExecutionContext<T> ctx = new LlmExecutionContext<>(request, provider, model, !isSingle);

            try {
                return tryExecute(ctx);
            } catch (LlmMissingTierConfigurationException e) {
                throw e;
            } catch (Exception e) {
                lastException = e;
                handleFailure(ctx, e);
            }
        }
        throw new LlmExecuteException("All providers failed", lastException);
    }

    private <T> LlmResponse<T> tryExecute(LlmExecutionContext<T> ctx) {
        if (ctx.useHealthCheck() && health.isBlocked(ctx.provider())) {
            return null;
        }

        LlmResponse<T> result = executionService.executeCall(new LlmRequest<>(
                chatClients.get(ctx.provider()),
                ctx.request().prompt(),
                ctx.provider(),
                ctx.model(),
                properties.common().temperature(),
                properties.common().maxCompletionTokens(),
                ctx.request().responseType()
        )).join();

        if (ctx.useHealthCheck()) {
            health.markSuccess(ctx.provider());
        }

        publishEvent(ctx.request(), result);
        return result;
    }

    private <T> void handleFailure(LlmExecutionContext<T> ctx, Exception e) {
        if (ctx.useHealthCheck()) {
            log.warn("LLM call failed for provider: {} [model: {}]. Error: {}. Falling back.",
                    ctx.provider(), ctx.model(), e.getMessage());
            health.markFailure(ctx.provider());
        } else {
            log.warn("LLM call failed for single provider: {} [model: {}]. Error: {}",
                    ctx.provider(), ctx.model(), e.getMessage());
        }
    }

    private <T> void publishEvent(LlmCallRequest<T> request, LlmResponse<T> result) {
        eventPublisher.publishEvent(new LlmExecutionEvent(
                request.correlationId(),
                request.correlationType(),
                request.source(),
                result.metrics(),
                request.prompt(),
                result.result()
        ));
    }

    private List<LlmProviderType> resolveFallbackOrder(LlmCallRequest<?> request) {
        return (request.providerFallbackOrderOverride() != null && !request.providerFallbackOrderOverride().isEmpty())
                ? request.providerFallbackOrderOverride()
                : properties.common().providerFallbackOrder();
    }

    private String resolveModel(LlmTierType tier, LlmProviderType provider) {
        Map<LlmProviderType, String> providerMap = properties.models().get(tier);
        if (providerMap == null) {
            String message = String.format("Configuration missing for tier: %s", tier);
            log.warn(message);
            throw new LlmMissingTierConfigurationException(message);
        }
        return providerMap.get(provider);
    }
}
