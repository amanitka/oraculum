package com.oraculum.llm.config;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Configuration
public class LlmResilienceConfig {

    @Bean
    public TimeLimiter llmTimeLimiter(LlmProperties properties) {
        return TimeLimiter.of(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(properties.common().timeout()))
                .cancelRunningFuture(true)
                .build());
    }

    @Bean
    public Retry llmRetry(LlmProperties properties) {

        Retry retry = Retry.of("llm", RetryConfig.custom()
                .maxAttempts(properties.common().maxRetries())
                .waitDuration(Duration.ofMillis(properties.common().retryDelayMs()))
                .retryExceptions(
                        IOException.class,
                        SocketTimeoutException.class
                )
                .build());

        retry.getEventPublisher()
                .onRetry(e -> log.warn("LLM retry #{} failed", e.getNumberOfRetryAttempts()))
                .onSuccess(e -> log.info("LLM success after {} attempts", e.getNumberOfRetryAttempts()))
                .onError(_ -> log.error("LLM failed permanently"));

        return retry;
    }

    @Bean
    public ExecutorService llmExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
