package com.oraculum.llm.service.impl;

import com.oraculum.llm.dto.LlmRequest;
import com.oraculum.llm.service.LlmExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LlmExecutionServiceImpl implements LlmExecutionService {

    @Retryable(includes = {java.io.IOException.class, java.net.SocketTimeoutException.class}, maxRetriesString =
            "#{@llmProperties.retry.max-retries}", delayString = "#{@llmProperties.retry.initial-backoff-ms}",
            multiplier = 2.0, jitter = 200)
    @Override
    public <T> T executeCall(LlmRequest<T> request) {
        return request.client()
                .prompt()
                .user(request.prompt())
                .options(OpenAiChatOptions.builder().model(request.model()).temperature(request.temperature()))
                .call()
                .entity(request.responseType());
    }
}
