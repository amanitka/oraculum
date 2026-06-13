package com.oraculum.llm.service.impl;


import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

@Component
@RequiredArgsConstructor
public class LlmResilienceExecutor {

    private final Retry llmRetry;
    private final TimeLimiter llmTimeLimiter;
    private final ExecutorService llmExecutor;

    public <T> T execute(Callable<T> task) {

        Callable<T> retryable = Retry.decorateCallable(llmRetry, task);
        Callable<T> timeLimited = TimeLimiter.decorateFutureSupplier(llmTimeLimiter, () -> llmExecutor.submit(retryable));

        try {
            return timeLimited.call();
        } catch (Exception e) {
            throw new RuntimeException("LLM execution failed", e);
        }
    }
}
