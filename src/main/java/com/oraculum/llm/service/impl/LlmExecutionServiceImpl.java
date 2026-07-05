package com.oraculum.llm.service.impl;

import com.oraculum.llm.api.dto.LlmMetrics;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.domain.LlmRequest;
import com.oraculum.llm.service.LlmExecutionService;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmExecutionServiceImpl implements LlmExecutionService {

    private ChatClient.CallResponseSpec getCallResponse(LlmRequest<?> request) {
        return request.client()
                .prompt()
                .user(request.prompt())
                .options(OpenAiChatOptions.builder()
                        .model(request.model())
                        .temperature(request.temperature())
                        .maxCompletionTokens(request.maxCompletionTokens()))
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

    @Async
    @Override
    @TimeLimiter(name = "llm")
    @Retry(name = "llm")
    public <T> CompletableFuture<LlmResponse<T>> executeCall(LlmRequest<T> request) {
        log.info("Executing LLM call [Provider: {}, Model: {}]", request.provider(), request.model());
        log.debug("Prompt:\n{}", request.prompt());
        long start = System.currentTimeMillis();
        var callResponse = getCallResponse(request);
        var responseEntity = callResponse.responseEntity(request.responseType());
        var entity = responseEntity.entity();
        var metrics = getMetrics(request, responseEntity.response(), start);
        log.info("LLM call completed in {} ms. Tokens - Prompt: {}, Completion: {}, Total: {}",
                metrics.latencyMs(), metrics.promptTokens(), metrics.completionTokens(), metrics.totalTokens());
        log.debug("Response:\n{}", entity);

        return CompletableFuture.completedFuture(new LlmResponse<>(entity, metrics));
    }
}
