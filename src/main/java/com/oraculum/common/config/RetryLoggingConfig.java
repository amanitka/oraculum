package com.oraculum.common.config;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.Retryable;
import org.springframework.resilience.annotation.EnableResilientMethods;

@Slf4j
@Configuration
@EnableResilientMethods
public class RetryLoggingConfig {

    @Bean
    public RetryListener retryListener() {
        return new RetryListener() {

            // beforeRetry() and onRetrySuccess() are handled by default interface methods

            @Override
            public void onRetryFailure(@NonNull RetryPolicy retryPolicy, @NonNull Retryable<?> retryable,
                                       @NonNull Throwable throwable) {
                log.warn("LLM request operation '{}' failed. Waiting before next attempt... Reason: [{}]",
                        retryable.getName(), throwable.getMessage());
            }
        };
    }
}