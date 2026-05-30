package com.oraculum.llm.service.impl;

import com.oraculum.llm.api.dto.LlmMetrics;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.domain.LlmRequest;
import com.oraculum.llm.service.LlmExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LlmExecutionServiceImpl implements LlmExecutionService {

    private ChatClient.CallResponseSpec getCallResponse(LlmRequest<?> request) {
        return request.client()
                .prompt()
                .user(request.prompt())
                .options(OpenAiChatOptions.builder().model(request.model()).temperature(request.temperature()))
                .call();
    }

    private LlmMetrics getMetrics(LlmRequest<?> request, ChatResponse chatResponse, long start) {
        Integer promptTokens = null;
        Integer completionTokens = null;
        Integer totalTokens = null;
        if (chatResponse != null) {
            var usage = chatResponse.getMetadata().getUsage();
            promptTokens = usage.getPromptTokens();
            completionTokens = usage.getCompletionTokens();
            totalTokens = usage.getTotalTokens();
        }

        return new LlmMetrics(request.provider(),
                request.model(),
                promptTokens,
                completionTokens,
                totalTokens,
                System.currentTimeMillis() - start);
    }

    @Retryable(includes = {java.io.IOException.class, java.net.SocketTimeoutException.class}, maxRetriesString =
            "$" + "{oraculum.llm.retry.max-retries}", delayString = "${oraculum.llm.retry.initial-backoff-ms}",
            multiplier = 2.0, jitter = 200)
    @Override
    public <T> LlmResponse<T> executeCall(LlmRequest<T> request) {
        long start = System.currentTimeMillis();
        var callResponse = getCallResponse(request);
        var responseEntity = callResponse.responseEntity(request.responseType());
        var entity = responseEntity.entity();
        var metrics = getMetrics(request, responseEntity.response(), start);

        return new LlmResponse<>(entity, metrics);
    }
}