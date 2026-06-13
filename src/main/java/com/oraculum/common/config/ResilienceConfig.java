package com.oraculum.common.config;

import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ResilienceConfig {

    @Bean
    public RegistryEventConsumer<Retry> myRetryRegistryEventConsumer() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(@NonNull EntryAddedEvent<Retry> entryAddedEvent) {
                String name = entryAddedEvent.getAddedEntry().getName();
                Retry retryInstance = entryAddedEvent.getAddedEntry();

                if ("llm".equals(name)) {
                    retryInstance.getEventPublisher()
                            .onRetry(e -> log.warn("LLM retry #{} failed", e.getNumberOfRetryAttempts()))
                            .onError(_ -> log.error("LLM failed permanently"));
                }
            }

            @Override
            public void onEntryRemovedEvent(@NonNull EntryRemovedEvent<Retry> entryRemovedEvent) {
            }

            @Override
            public void onEntryReplacedEvent(@NonNull EntryReplacedEvent<Retry> entryReplacedEvent) {
            }
        };
    }
}
